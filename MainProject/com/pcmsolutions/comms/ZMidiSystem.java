/*
 * ZMidiSystem.java
 *
 * Created on November 12, 2002, 12:22 AM
 */

package com.pcmsolutions.comms;

import com.pcmsolutions.device.EMU.E4.RemoteUnreachableException;
import com.pcmsolutions.gui.ProgressSession;
import com.pcmsolutions.system.ZDisposable;
import com.pcmsolutions.system.ZUtilities;
import com.pcmsolutions.system.Zoeos;
import com.pcmsolutions.system.preferences.Impl_ZBoolPref;
import com.pcmsolutions.system.preferences.ZBoolPref;
import com.pcmsolutions.system.threads.ZDefaultThread;
import com.pcmsolutions.system.threads.Impl_ZThread;
import com.pcmsolutions.util.CALock;
import com.pcmsolutions.util.RWLock;

import javax.sound.midi.*;
import javax.swing.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;


/**
 * @author pmeehan
 */
public class ZMidiSystem implements ZDisposable {
    // private static volatile int TRANSACTION_PRIORITY = 5;

    public static final CALock midiLock = new CALock();
    final HashMap<MidiDevice.Info, ZMidiDevice> deviceMap = new HashMap<>();
    static final CopyOnWriteArrayList listeners = new CopyOnWriteArrayList();

    // PREFERENCE STUFF
    public static final int CONCURRENCY_LOW = 0;
    public static final int CONCURRENCY_MEDIUM = 1;
    public static final int CONCURRENCY_HIGH = 2;
    static final String PREF_sysexConcurrencyLevel = "sysexConcurrencyLevel";
    //public static final Preferences prefs = Preferences.userNodeForPackage(ZMidiSystem.class).node("ZMidiSystem");

    // CONCURRENCY
    static final /*volatile*/ int concurrencyLevel = CONCURRENCY_LOW;

    static {
        //getSysexConcurrencyLevel();
    }

    final MidiInputQ midiIn_q = new MidiInputQ();

    static class MidiInputQ {
        private final ArrayList<FinalMidiMessage> queue = new ArrayList<>(2000);
        private int mark = 0;

        public synchronized void addMsg(FinalMidiMessage msg) {
            if (msg != null) {
                queue.add(msg);
                mark++;
                notifyAll();
            }
        }

        public synchronized int mark() {
            return mark;
        }

        public int filterMidiIn_q(Filterable filter, MidiDevice.Info source, int mark, List client_q) {
            return filterMidiIn_q(new Filterable[]{filter}, source, mark, client_q);
        }

        public synchronized int filterMidiIn_q(Filterable[] filters, MidiDevice.Info source, int prevMark, List client_q) {
            Object filt_msg;
            FinalMidiMessage m;
            int searchIndex = queue.size() - (mark - prevMark);

            if (searchIndex < 0)
                searchIndex = 0;

            for (int n = searchIndex, p = queue.size(); n < p; n++) {
                m = queue.get(n);
                if (m != null && m.getSource().equals(source)) {
                    for (int f = 0; f < filters.length; f++)
                        if (filters[f] != null) {
                            filt_msg = filters[f].filter(m);
                            if (filt_msg != null) {
                                client_q.add(filt_msg);
                                m.addMatchRef();
                                break;
                                // if (m.getMatchRef() > 1)
                                //     System.out.println("msg ref > 1");
                            }
                        }
                }
            }
            return mark;
        }

        static volatile int retentionDaemonSleepTime = 10000;
        ZDefaultThread retentionDaemon = new ZDefaultThread("Midi message retention daemon") {
            @Override
            public void runBody() {
                int nextClipIndex = -1;
                while (shouldRun) {
                    try {
                        try {
                            Thread.sleep(retentionDaemonSleepTime);
                        } catch (InterruptedException e) {
                        }
                        synchronized (MidiInputQ.this) {
                            try {
                                if (nextClipIndex != -1) {
                                    List<FinalMidiMessage> rem = new ArrayList<>(queue.subList(nextClipIndex, queue.size()));
                                    queue.clear();
                                    queue.addAll(rem);
                                    //System.out.println("MARK = " + mark());
                                }
                            } finally {
                                nextClipIndex = queue.size();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        {
            retentionDaemon.start();
        }
    }

    static FinalMidiMessage finalizeMidiMessage(MidiMessage m, MidiDevice.Info source) {
        if (m instanceof SysexMessage)
            return new Sysex(m, source);
        if (m instanceof ShortMessage)
            return new Short(m, source);
        return null;
    }


    static final RWLock globalSysexLock = new RWLock();
    static final RWLock secondaryGlobalSysexLock = new RWLock();

    /*
     public static void setSysexConcurrencyLevel(int level) {
         midiLock.configure();
         try {
             concurrencyLevel = level;
             if (concurrencyLevel < CONCURRENCY_LOW)
                 concurrencyLevel = CONCURRENCY_LOW;
             else if (concurrencyLevel > CONCURRENCY_HIGH)
                 concurrencyLevel = CONCURRENCY_HIGH;
             prefs.putInt(PREF_sysexConcurrencyLevel, concurrencyLevel);
             getInstance().fireMidiSystemChanged();
         } finally {
             midiLock.releaseContent();
         }
     }

     public static int getSysexConcurrencyLevel() {
         return (concurrencyLevel = prefs.getInt(PREF_sysexConcurrencyLevel, CONCURRENCY_LOW));
     }
     */
    static void beginShortSysexTransaction() throws IllegalArgumentException {
        midiLock.access();
        try {
            switch (concurrencyLevel) {
                case CONCURRENCY_LOW:
                    globalSysexLock.write();
                    return;
                case CONCURRENCY_MEDIUM:
                case CONCURRENCY_HIGH:
                    globalSysexLock.read();
                    return;
                default:
                    throw new IllegalArgumentException("invalid concurrency level");

            }
        } catch (IllegalArgumentException e) {
            midiLock.unlock();
            throw e;
        }
    }

    static void endShortSysexTransaction() {
        try {
            midiLock.unlock();
        } finally {
            globalSysexLock.unlock();
        }
    }

    static void beginLongSysexTransaction() throws IllegalArgumentException {
        midiLock.access();
        try {
            switch (concurrencyLevel) {
                case CONCURRENCY_LOW:
                    globalSysexLock.write();
                    try {
                        secondaryGlobalSysexLock.write();
                    } catch (IllegalArgumentException e) {
                        globalSysexLock.unlock();
                        throw e;
                    }
                    return;
                case CONCURRENCY_MEDIUM:
                    globalSysexLock.read();
                    try {
                        secondaryGlobalSysexLock.write();
                    } catch (IllegalArgumentException e) {
                        globalSysexLock.unlock();
                    }
                    return;
                case CONCURRENCY_HIGH:
                    globalSysexLock.read();
                    try {
                        secondaryGlobalSysexLock.read();
                    } catch (IllegalArgumentException e) {
                        globalSysexLock.unlock();
                    }
                    return;
                default:
                    throw new IllegalArgumentException("invalid concurrency level");
            }
        } catch (IllegalArgumentException e) {
            midiLock.unlock();
            throw e;
        }
    }

    static void endLongSysexTransaction() {
        try {
            midiLock.unlock();
        } finally {
            try {
                globalSysexLock.unlock();
            } finally {
                secondaryGlobalSysexLock.unlock();
            }
        }
    }

    /**
     * Creates a new instance of ZMidiSystem
     */
    ZMidiSystem() {
        refreshMidiDeviceInfo(true);
    }

    public interface MidiSystemListener {
        public void midiSystemChanged(ZMidiSystem msf);
    }

    public static void addMidiSystemListener(MidiSystemListener msl) {
        listeners.add(msl);
    }

    public static void removeMidiSystemListener(MidiSystemListener msl) {
        listeners.remove(msl);
    }

    void fireMidiSystemChanged() {
        final CopyOnWriteArrayList listeners_clone = (CopyOnWriteArrayList) listeners.clone();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (Iterator i = listeners_clone.iterator(); i.hasNext();)
                    ((MidiSystemListener) i.next()).midiSystemChanged(ZMidiSystem.this);
            }
        });
    }

    public void zDispose() {
        try {
            synchronized (deviceMap) {
                for (Iterator i = deviceMap.values().iterator(); i.hasNext();) {
                    Object d = i.next();
                    if (d instanceof ZMidiDevice)
                        ((ZMidiDevice) d).referencedClose();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refresh(boolean visualFeedback) {
        refreshMidiDeviceInfo(visualFeedback);
    }

    ZMidiDevice getDevice(MidiDevice.Info dev) throws MidiUnavailableException {
        ZMidiDevice zd = deviceMap.get(dev);
        if (zd == null) throw new MidiUnavailableException();
        return zd;
    }
    /*
    public void setPortNeverToBeClosed(MidiDevice.Info devInfo, boolean val) throws MidiUnavailableException {
        ZMidiDevice zd = getDevice(devInfo);
        zd.setNeverCloses(val);
        fireMidiSystemChanged();
    }

    public void togglePortNeverToBeClosed(MidiDevice.Info devInfo) throws MidiUnavailableException {
        ZMidiDevice zd = getDevice(devInfo);
        zd.setNeverCloses(!zd.getNeverCloses());
        fireMidiSystemChanged();
    }

    public boolean isPortNeverToBeClosed(MidiDevice.Info devInfo) throws MidiUnavailableException {
        return getDevice(devInfo).getNeverCloses();
    }

    */
    public void permitAll() {

    }

    public void setPortPermitted(MidiDevice.Info devInfo, boolean val) throws MidiUnavailableException {
        ZMidiDevice zd = getDevice(devInfo);
        zd.setPermitted(val);
        fireMidiSystemChanged();
    }

    public void togglePortPermitted(MidiDevice.Info devInfo) throws MidiUnavailableException {
        ZMidiDevice zd = getDevice(devInfo);
        zd.setPermitted(!zd.getPermitted());
        fireMidiSystemChanged();
    }

    public boolean isPortPermitted(MidiDevice.Info devInfo) throws MidiUnavailableException {
        return getDevice(devInfo).getPermitted();
    }

    void refreshMidiDeviceInfo(final boolean visualFeedback) {
        midiLock.access();
        try {
            synchronized (deviceMap) {
                MidiDevice.Info[] devices;
                devices = MidiSystem.getMidiDeviceInfo();
                Map<MidiDevice.Info, ZMidiDevice> dm_clone = (Map<MidiDevice.Info, ZMidiDevice>) deviceMap.clone();
                deviceMap.clear();
                Zoeos z = Zoeos.getInstance();
                ProgressSession ps = null;
                if (visualFeedback)
                //z.beginProgressElement(this, "Initializing Midi System", devices.length);
                    ps = z.getProgressSession("Initializing Midi System", devices.length);
                MidiDevice.Info info;
                try {
                    for (int n = 0; n < devices.length; n++) {
                        try {
                            info = devices[n];
                            if (!dm_clone.containsKey(info)) {
                                deviceMap.put(info, new ZMidiDevice(info));
                            } else
                                deviceMap.put(info, dm_clone.get(info));
                        } finally {
                            if (visualFeedback)
                                ps.updateStatus();
                        }
                    }
                } finally {
                    if (visualFeedback)
                        ps.end();
                }
            }
        } finally {
            midiLock.unlock();
            this.fireMidiSystemChanged();
        }
    }

    static ZMidiSystem instance;

    public static ZMidiSystem getInstance() {
        if (instance == null)
            instance = new ZMidiSystem();
        return instance;
    }

    public MidiDevice.Info[] getPermittedDevices() {
        ArrayList<MidiDevice.Info> permitted = new ArrayList<MidiDevice.Info>();
        MidiDevice.Info dev;
        synchronized (deviceMap) {
            Iterator<MidiDevice.Info> i = deviceMap.keySet().iterator();
            while (i.hasNext()) {
                dev = i.next();
                try {
                    if (isPortPermitted(dev))
                        permitted.add(dev);
                } catch (MidiUnavailableException e) {
                    e.printStackTrace();
                }
            }
        }
        Collections.sort(permitted, new Comparator<MidiDevice.Info>() {
            public int compare(MidiDevice.Info info, MidiDevice.Info info1) {
                return (info.getName() + info.getDescription() + info.getVendor() + info.getVersion()).compareTo((info1.getName() + info1.getDescription() + info1.getVendor() + info1.getVersion()));
            }
        });
        return (MidiDevice.Info[]) permitted.toArray(new MidiDevice.Info[permitted.size()]);
    }

    public MidiDevice.Info[] getAllDevices() {
        synchronized (deviceMap) {
            return (MidiDevice.Info[]) deviceMap.keySet().toArray(new MidiDevice.Info[deviceMap.size()]);
        }
    }

    public Inlet getInlet(MidiDevice.Info i, Object owner, String name) throws MidiUnavailableException, MidiDeviceNotPermittedException {
        return getDevice(i).getInlet(owner, name);
    }

    public Outlet getOutlet(MidiDevice.Info i, Object owner, String name) throws MidiUnavailableException, MidiDeviceNotPermittedException {
        return getDevice(i).getOutlet(owner, name);
    }

    public PausingOutlet getPausingOutlet(MidiDevice.Info i, Object owner, String name) throws MidiUnavailableException, MidiDeviceNotPermittedException {
        return getDevice(i).getPausingOutlet(owner, name);
    }

    public BufferedInlet getBufferedInlet(MidiDevice.Info i, Object owner, String name) throws MidiUnavailableException, MidiDeviceNotPermittedException {
        return getDevice(i).getBufferedInlet(owner, name);
    }

    public static DeviceHunter getDeviceHunter() {
        return Impl_DeviceHunter.getInstance();
    }

    public int getRefCount(MidiDevice.Info devInfo) throws MidiUnavailableException {
        return getDevice(devInfo).numRefs();
    }

    static final String[] defaultIgnores = new String[]{"Broadcast", "Control", "Sync", "Wavetable", "Microsoft", "Java Sound", "MIDI Mapper"};

    class ZMidiDevice {
        final private MidiDevice.Info devInfo;

        //ZBoolPref ZPREF_neverClose;
        ZBoolPref ZPREF_permitted;

        private final Receiver deviceReceiver = new Receiver() {
            public void send(javax.sound.midi.MidiMessage midiMessage, long param) {
                try {
                    midiIn_q.addMsg(finalizeMidiMessage(midiMessage, devInfo));
                    //System.out.println("msg received");
                    // NOTE: MIDI THREAD SEEMS TO HAVE PRIORITY OF 6
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void close() {
                System.out.println("Call to referencedClose on ZDevice receiver!!");
            }
        };

        private ZMidiDevice(MidiDevice.Info di) {
            devInfo = di;
           // ZPREF_neverClose = new Impl_ZBoolPref(Preferences.userNodeForPackage(ZMidiDevice.class), devInfo.getName()
           //         + "_" + devInfo.getDescription() + "_neverClose", false, "Never referencedClose port", "");            //getRealDevice();
            ZPREF_permitted = new Impl_ZBoolPref(Preferences.userNodeForPackage(ZMidiDevice.class.getClass()), devInfo.getName()
                    + "_" + devInfo.getDescription() + "_permitted", getDefaultPermission(), "Permitted", "");            //getRealDevice();
        }

        public int numRefs() throws MidiUnavailableException {
            // if (getRealDevice() == null)
            //    return 0;
            //else
            return getRealDevice().getReceivers().size() + getRealDevice().getTransmitters().size();
        }

        /*
        public boolean getNeverCloses() {
            return ZPREF_neverClose.getValue();
        }

        public void setNeverCloses(boolean val) {
            ZPREF_neverClose.putValue(val);
        }
        */
        public boolean getPermitted() {
            return ZPREF_permitted.getValue();
        }

        public void setPermitted(boolean val) {
            ZPREF_permitted.putValue(val);
        }

        private boolean getDefaultPermission() {
            String name = devInfo.getName();
            for (String s : defaultIgnores)
            if (name.contains(s))
                return false;
            return true;
        }

        public MidiDevice getRealDevice() throws MidiUnavailableException {
            return MidiSystem.getMidiDevice(devInfo);
        }

        //private static final int OPEN_CLOSE_WAIT_TIME = 2000;
        /*
        ZTimedWaitThread t = new ZTimedWaitThread(OPEN_CLOSE_WAIT_TIME) {
            public boolean runBody() {
                realDevice.referencedClose();
                realDevice = null;
                return true;
            }
        };
        t.start();
        lastCloseSuccessful = t.isSuceeded();
        */

        public void assertOpen() throws MidiUnavailableException {
            midiLock.access();
            try {
                if (getRealDevice().isOpen()) {
                    System.out.println("Midi device already open: " + getRealDevice().getDeviceInfo().getName());
                } else {
                    getRealDevice().open();
                    System.out.println("Midi device opened: " + getRealDevice().getDeviceInfo().getName());
                }
            } finally {
                midiLock.unlock();
            }
        }

        public void referencedClose() throws MidiUnavailableException {
            midiLock.access();
            try {
                //if (  false){//getRealDevice().getMaxTransmitters() == 0) {
                try {
                    if (getRealDevice().isOpen())
                        if ( ((getRealDevice().getTransmitters() != null) && (getRealDevice().getTransmitters().isEmpty() )) &&
                              ((getRealDevice().getReceivers() != null) && (getRealDevice().getReceivers().isEmpty() ))  )
                        {
                        try {
                            System.out.println("Midi device closing: " + getRealDevice().getDeviceInfo().getName());
                            getRealDevice().close();
                            System.out.println("Midi device closed: " + getRealDevice().getDeviceInfo().getName());

                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                } catch (MidiUnavailableException e) {
                    System.out.println(e);
                }
                //  }else
                //     System.out.println("Midi device left opened: " + getRealDevice().getDeviceInfo().getName());
            } finally {
                midiLock.unlock();
            }
        }

        private Transmitter requestTransmitter() throws MidiUnavailableException {
            midiLock.access();
            try {
                final Transmitter t = getRealDevice().getTransmitter();
                assertOpen();
                fireMidiSystemChanged();
                return new Transmitter() {
                    public void setReceiver(Receiver receiver) {
                        t.setReceiver(receiver);
                    }

                    public Receiver getReceiver() {
                        return t.getReceiver();
                    }

                    public void close() {

                        t.close();
                        try {
                            ZMidiDevice.this.referencedClose();
                        } catch (MidiUnavailableException e) {
                            e.printStackTrace();
                        }

                        fireMidiSystemChanged();

                    }
                };
            } finally {
                midiLock.unlock();
            }
        }

        private Receiver requestReceiver() throws MidiUnavailableException {
            midiLock.access();
            try {
                final Receiver r = getRealDevice().getReceiver();
                assertOpen();
                fireMidiSystemChanged();
                return new Receiver() {
                    public void send(MidiMessage message, long timeStamp) {
                        r.send(message, timeStamp);
                    }

                    public void close() {
                        r.close();
                        try {
                            ZMidiDevice.this.referencedClose();
                        } catch (MidiUnavailableException e) {
                            e.printStackTrace();
                        }
                        fireMidiSystemChanged();
                    }
                };
            } finally {
                midiLock.unlock();
            }
        }

        Inlet getInlet(Object owner, String name) throws MidiUnavailableException, MidiDeviceNotPermittedException {
            if (getPermitted())
                return new Impl_Inlet(owner, name);
            else
                throw new MidiDeviceNotPermittedException(devInfo);
        }

        private BufferedInlet getBufferedInlet(Object owner, String name) throws MidiUnavailableException, MidiDeviceNotPermittedException {
            if (getPermitted())
                return new Impl_BufferedInlet(owner, name);
            else
                throw new MidiDeviceNotPermittedException(devInfo);
        }

        private Outlet getOutlet(Object owner, String name) throws MidiUnavailableException, MidiDeviceNotPermittedException {
            if (getPermitted())
                return new Impl_Outlet(owner, name);
            else
                throw new MidiDeviceNotPermittedException(devInfo);
        }

        private PausingOutlet getPausingOutlet(Object owner, String name) throws MidiUnavailableException, MidiDeviceNotPermittedException {
            if (getPermitted())
                return new Impl_PausingOutlet(owner, name);
            else
                throw new MidiDeviceNotPermittedException(devInfo);
        }

        abstract class Impl_AbstractInlet {
            Object owner;
            String name;
            Transmitter xmit;

            public Impl_AbstractInlet(Object owner, String name) throws MidiUnavailableException {
                this.owner = owner;
                this.name = name;
                xmit = requestTransmitter();
                xmit.setReceiver(deviceReceiver);
                System.out.println(Zoeos.getZoeosTime() + ": " + "INLET created on midi device : " + getRealDevice().getDeviceInfo());
            }

            public void discard() {
                xmit.close();
            }

            /*public Transmitter getXmit() throws MidiUnavailableException {
                if (xmit == null) {
                    xmit = requestTransmitter();
                    xmit.setReceiver(deviceReceiver);
                }
                return xmit;
            }
            */
            public MidiDevice.Info getDeviceInfo() throws MidiUnavailableException {
                return getRealDevice().getDeviceInfo();
            }

            public ZMidiDevice getDevice() {
                return ZMidiDevice.this;
            }

            public Object getOwner() {
                return owner;
            }
        }

        class Impl_Inlet extends Impl_AbstractInlet implements Inlet {
            private static final int defTimeout = 1000;     // ms
            private volatile int timeout = defTimeout;
            private final List client_q = new ArrayList();

            private Impl_Inlet(Object owner, String name) throws MidiUnavailableException {
                super(owner, name);
            }

            public void setTimeout(int val) {
                timeout = val;
            }

            public Object[] dispatchAndWaitForLongReply(Outlet o, MidiMessage m, Filterable filter) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException, RemoteDeviceDidNotRespondException, RemoteMessagingException {
                Object[] replies = dispatchAndWaitForLongReplies(o, m, filter, 1);
                if (replies != null && replies.length > 0)
                    return replies;
                return null;
            }

            public Object dispatchAndWaitForReply(Outlet o, MidiMessage m, Filterable filter) throws RemoteMessagingException, RemoteDeviceDidNotRespondException, RemoteUnreachableException {
                Object[] replies = new Object[0];
                replies = dispatchAndWaitForReplies(o, m, filter, 1);
                if (replies != null && replies.length > 0)
                    return replies[0];
                return null;
            }

            public Object[] dispatchAndWaitForReplies(Outlet o, MidiMessage m, Filterable filter, int numReplies) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException, RemoteDeviceDidNotRespondException, RemoteMessagingException {
                return dispatchAndWaitForReplies(o, m, filter, numReplies, false);
            }

            public Object[] dispatchAndWaitForLongReplies(Outlet o, MidiMessage m, Filterable filter, int numReplies) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException, RemoteDeviceDidNotRespondException, RemoteMessagingException {
                return dispatchAndWaitForReplies(o, m, filter, numReplies, true);
            }

            private Object[] dispatchAndWaitForReplies(Outlet o, MidiMessage m, Filterable filter, int numReplies, final boolean longReplies) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException, RemoteDeviceDidNotRespondException, RemoteMessagingException {
                return taskDispatchAndWaitForReplies(o, m, filter, numReplies, longReplies, 1);
            }

            public Object[] taskDispatchAndWaitForReplies(Outlet o, MidiMessage m, Filterable filter, final int reqdReplies, final boolean longReplies, final int retries) throws RemoteUnreachableException, RemoteMessagingException, RemoteDeviceDidNotRespondException {
                return dispatchAndWaitForRepliesScheme1(o, m, filter, reqdReplies, longReplies);

            }

            private Object[] dispatchAndWaitForRepliesScheme1(final Outlet o, final MidiMessage m, final Filterable filter, final int reqdReplies, final boolean longReplies) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException, RemoteDeviceDidNotRespondException, RemoteMessagingException {
                if (reqdReplies < 1)
                    throw new IllegalArgumentException("Request for zero replies in inlet");
                try {
                    if (longReplies)
                        ZMidiSystem.beginLongSysexTransaction();
                    else
                        ZMidiSystem.beginShortSysexTransaction();
                    try {
                        if (xmit == null)
                            throw new com.pcmsolutions.device.EMU.E4.RemoteUnreachableException("Midi transmitter not available");
                        client_q.clear();
                        synchronized (midiIn_q) {
                            int mark = midiIn_q.mark();
                            o.dispatch(m, -1);
                            final long startTime = Zoeos.getZoeosTime();
                            long remTime = timeout;
                            do {
                                try {
                                    midiIn_q.wait(remTime);
                                } catch (InterruptedException e) {
                                }
                                mark = midiIn_q.filterMidiIn_q(filter, devInfo, mark, client_q);

                                remTime = timeout - (Zoeos.getZoeosTime() - startTime);
                            } while (remTime > 0 && reqdReplies > client_q.size());
                        }
                        return analyzeQueues(m, reqdReplies);
                    } finally {
                        if (longReplies)
                            ZMidiSystem.endLongSysexTransaction();
                        else
                            ZMidiSystem.endShortSysexTransaction();
                    }
                } catch (IllegalArgumentException e) {
                    throw new com.pcmsolutions.device.EMU.E4.RemoteUnreachableException("Midi locking error");
                } finally {
                }
            }

            private Object[] analyzeQueues(MidiMessage m, int reqdReplies) throws RemoteDeviceDidNotRespondException, RemoteMessagingException {
                if (client_q.size() >= reqdReplies) {
                    if (client_q.size() > reqdReplies)
                        System.out.println("more than requested, needed " + reqdReplies + " but got " + client_q.size());
                    return client_q.toArray();
                } else if (client_q.size() == 0)
                    throw new RemoteDeviceDidNotRespondException("No response(s) to message: " + ZUtilities.getByteString(m.getMessage()) + Zoeos.lineSeperator);
                else {
                    throw new RemoteMessagingException("Insufficient number of responses to message: " + ZUtilities.getByteString(m.getMessage()) + Zoeos.lineSeperator + "required " + reqdReplies + ", received " + client_q.size());
                }
            }
        }

        private class Impl_BufferedInlet extends Impl_AbstractInlet implements BufferedInlet {
            protected final ArrayList filters = new ArrayList();
            protected int lastMark = midiIn_q.mark();

            private Impl_BufferedInlet(Object owner, String name) throws MidiUnavailableException {
                super(owner, name);
            }

            public List clearBuffer() {
                ArrayList buffer = new ArrayList();
                int fsz = filters.size();
                if (fsz == 0)
                    return buffer;

                lastMark = midiIn_q.filterMidiIn_q((Filterable[]) filters.toArray(new Filterable[filters.size()]), devInfo, lastMark, buffer);
                return buffer;
            }

            public void addFilter(Filterable o) {
                filters.add(o);
            }

            public void removeFilter(Filterable o) {
                filters.remove(o);
            }

            public List getFilters() {
                return (List) filters.clone();
            }
        }

        private class Impl_Outlet implements Outlet {
            Object owner;
            String name;
            Receiver recv;

            private Impl_Outlet(Object owner, String name) throws MidiUnavailableException {
                this.owner = owner;
                this.name = name;
                recv = requestReceiver();
                System.out.println(Zoeos.getZoeosTime() + ": " + "OUTLET Created on Midi Device : " + getRealDevice().getDeviceInfo());
            }

            public Object getOwner() {
                return owner;
            }

            public MidiDevice.Info getDeviceInfo() throws MidiUnavailableException {
                return getRealDevice().getDeviceInfo();
            }

            public void dispatch(MidiMessage midiMessage, long param) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException {
                midiLock.access();
                try {
                    try {
                        recv.send(midiMessage, param);
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                } finally {
                    midiLock.unlock();
                }
            }

            public void discard() {
                recv.close();
                recv = null;
            }
        }

        private class Impl_PausingOutlet extends Impl_Outlet implements PausingOutlet {
            private Impl_ZThread timer;
            private volatile long pause = 0;
            private boolean isPaused = false;

            // passed pause ignored for now
            private Impl_PausingOutlet(Object owner, String name) throws MidiUnavailableException {
                super(owner, name);
                timer = new TimerThread();
                timer.start();
            }

            public void setPause(long pause) {
                this.pause = pause;
            }

            private class TimerThread extends Impl_ZThread {
                private Object start_mon = new Object();
                private boolean running = false;

                public void start() {
                    synchronized (start_mon) {
                        super.start();
                        while (running == false)
                            try {
                                start_mon.wait();
                            } catch (InterruptedException e) {
                            }
                    }
                }

                public void runBody() {
                    setName("Pausing Outlet Timer Thread");
                    synchronized (start_mon) {
                        running = true;
                        start_mon.notify(); //notify starter of thread that it is now running
                    }
                    synchronized (this) {
                        while (shouldRun) {
                            try {
                                wait();
                                Thread.sleep(pause);
                                synchronized (Impl_PausingOutlet.this) {
                                    isPaused = false;
                                    Impl_PausingOutlet.this.notifyAll();
                                }
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                }
            }

            public void discard() {
                try {
                    super.discard();
                } finally {
                    timer.stopThreadSafely();
                }
            }

            public void dispatch(MidiMessage midiMessage, long param, long pause) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException {
                midiLock.access();
                this.pause = pause;
                try {
                    if (recv == null)
                        throw new com.pcmsolutions.device.EMU.E4.RemoteUnreachableException("Midi receiver not available");
                    synchronized (this) {
                        while (isPaused) {
                            try {
                                wait();
                            } catch (Exception e) {
                            }
                        }
                        try {
                            recv.send(midiMessage, param);
                            if (pause != 0) {
                                isPaused = true;
                                synchronized (timer) {
                                    timer.notify();
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                } finally {
                    midiLock.unlock();
                }
            }

            public void dispatch(MidiMessage midiMessage, long param) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException {
                dispatch(midiMessage, param, 0);
            }
        }
    }

    public interface Inlet {
        public Object getOwner();

        public void discard();

        public MidiDevice.Info getDeviceInfo() throws MidiUnavailableException;

        public ZMidiDevice getDevice();

        public void setTimeout(int val);

        public Object dispatchAndWaitForReply(Outlet o, MidiMessage m, Filterable filter) throws RemoteMessagingException, RemoteDeviceDidNotRespondException, RemoteUnreachableException;

        public Object[] dispatchAndWaitForLongReply(Outlet o, MidiMessage m, Filterable filter) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException, RemoteDeviceDidNotRespondException, RemoteMessagingException;

        public Object[] dispatchAndWaitForReplies(Outlet o, MidiMessage m, Filterable filter, int numReplies) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException, RemoteDeviceDidNotRespondException, RemoteMessagingException;

        public Object[] dispatchAndWaitForLongReplies(Outlet o, MidiMessage m, Filterable filter, int numReplies) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException, RemoteDeviceDidNotRespondException, RemoteMessagingException;
    }

    public interface BufferedInlet {

        public Object getOwner();

        public void discard();

        public MidiDevice.Info getDeviceInfo() throws MidiUnavailableException;

        public List clearBuffer();

        public void addFilter(Filterable o);

        public void removeFilter(Filterable o);

        public List getFilters();
    }

    public interface Outlet {
        public Object getOwner();

        public MidiDevice.Info getDeviceInfo() throws MidiUnavailableException;

        public void dispatch(MidiMessage midiMessage, long param) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException;

        public void discard();
    }

    public interface PausingOutlet extends ZMidiSystem.Outlet {
        public void dispatch(MidiMessage midiMessage, long param, long pause) throws com.pcmsolutions.device.EMU.E4.RemoteUnreachableException;
    }

    /**
     * @author pmeehan
     */
    public static interface DeviceHunter {
        public List hunt(int id, MidiDevice.Info[] devices, int timeout);

        public List hunt(int id, MidiDevice.Info[] devices);

        public List hunt(int id, int timeout);

        public List hunt(int id);
    }

    static class Impl_DeviceHunter implements DeviceHunter {
        private static final Impl_DeviceHunter INSTANCE = new Impl_DeviceHunter();
        private static final int defTimeout = 300;
        private int timeout = defTimeout;
        private Set huntOutlets = new HashSet();
        private Set huntInlets = new HashSet();
        private Outlet currOutlet = null;

        private Impl_DeviceHunter() {
        }

        public String toString() {
            return "Device Hunter";
        }

        public static DeviceHunter getInstance() {
            return INSTANCE;
        }

        public List hunt(int id, MidiDevice.Info[] devInfos, int timeout) {
            ZMidiSystem.midiLock.configure();
            try {
                prepare(devInfos, timeout);
                List replies = performHunt(id);
                cleanup();
                return replies;
            } finally {
                ZMidiSystem.midiLock.unlock();
            }
        }

        public List hunt(int id, MidiDevice.Info[] devInfos) {
            ZMidiSystem.midiLock.configure();
            try {
                prepare(devInfos, defTimeout);
                List replies = performHunt(id);
                cleanup();
                return replies;
            } finally {
                ZMidiSystem.midiLock.unlock();
            }
        }

        public List hunt(int id, int timeout) {
            ZMidiSystem ms = ZMidiSystem.getInstance();
            ZMidiSystem.midiLock.configure();
            try {
                prepare(ms.getPermittedDevices(), timeout);
                List replies = performHunt(id);
                cleanup();
                return replies;
            } finally {
                ZMidiSystem.midiLock.unlock();
            }
        }

        public List hunt(int id) {
            ZMidiSystem ms = ZMidiSystem.getInstance();
            ZMidiSystem.midiLock.configure();
            try {
                prepare(ms.getPermittedDevices(), defTimeout);

                List replies = performHunt(id);
                cleanup();

                return replies;
            } finally {
                ZMidiSystem.midiLock.unlock();
            }
        }

        private void prepare(MidiDevice.Info[] devInfos, int timeout) {
            ZMidiSystem ms = ZMidiSystem.getInstance();
            MidiDevice.Info d;
            for (int i = 0, j = devInfos.length; i < j; i++) {
                d = devInfos[i];
                try {
                    BufferedInlet inlet = ms.getBufferedInlet(d, this, "Hunt Port");
                    inlet.addFilter(new IdentityReply());
                    huntInlets.add(inlet);
                    //System.out.println(Zoeos.getZoeosTime() + ": " + "Found in port:" + inlet.getDeviceInfo().toString());
                } catch (MidiUnavailableException e) {
                    //e.printStackTrace();
                } catch (MidiDeviceNotPermittedException e) {
                    e.printStackTrace();
                }
                try {
                    Outlet o = ms.getOutlet(d, this, "Hunt Port");
                    huntOutlets.add(o);
                    //System.out.println(Zoeos.getZoeosTime() + ": " + "Found out port:" + o.getDeviceInfo().toString());
                } catch (MidiUnavailableException e) {
                    //  e.printStackTrace();
                } catch (MidiDeviceNotPermittedException e) {
                    e.printStackTrace();
                }
            }

            this.timeout = timeout;
        }

        private List performHunt(int id) {
            Zoeos z = Zoeos.getInstance();
            List replies = new ArrayList();

            ProgressSession ps = z.getProgressSession("Hunting for Devices...", huntOutlets.size());
            try {
                IdentityRequest msg = new IdentityRequest((byte) id);

                for (Iterator i = huntOutlets.iterator(); i.hasNext();) {
                    currOutlet = (Outlet) i.next();

                    currOutlet.dispatch(msg, -1);
                    System.out.println(Zoeos.getZoeosTime() + ": " + "Sending Identity Request on port:" + currOutlet.getDeviceInfo().toString() + " ; while listening on  " + huntInlets.size() + " ports");
                    try {
                        Thread.sleep(timeout);
                    } catch (IllegalStateException e) {
                        System.out.println(e);
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }

                    for (Iterator j = huntInlets.iterator(); j.hasNext();) {
                        final BufferedInlet inlet = (BufferedInlet) j.next();
                        final List buf = inlet.clearBuffer();

                        for (Iterator k = buf.iterator(); k.hasNext();) {
                            Object o = k.next();
                            final IdentityReply irm = (IdentityReply) o;
                            final IdentityRequest reqMsg = (IdentityRequest) msg.clone();
                            final MidiDevice.Info inInfo = inlet.getDeviceInfo();
                            final MidiDevice.Info outInfo = currOutlet.getDeviceInfo();
                            System.out.println("REPLY:");
                            System.out.println(irm);
                            System.out.println("OUT PORT->" + outInfo.toString());
                            System.out.println("IN PORT->" + inInfo.toString());
                            replies.add(new SysexTransactionRecord() {
                                public MidiDevice.Info getOutDeviceInfo() {
                                    return outInfo;
                                }

                                public MidiDevice.Info getInDeviceInfo() {
                                    return inInfo;
                                }

                                public FinalMidiMessage getReply() {
                                    return irm;
                                }

                                public IdentityRequest getRequest() {
                                    return reqMsg;
                                }
                            });
                        }
                    }
                    ps.updateStatus();
                }
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                ps.end();
            }
            return replies;
        }

        private void cleanup() {
            for (Iterator i = huntInlets.iterator(); i.hasNext();)
                ((BufferedInlet) i.next()).discard();

            huntInlets.clear();

            try {
                for (Iterator i = huntOutlets.iterator(); i.hasNext();)
                    ((Outlet) i.next()).discard();
                huntOutlets.clear();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

}
