package com.pcmsolutions.system;

import com.pcmsolutions.comms.SysexTransactionRecord;
import com.pcmsolutions.comms.ZMidiSystem;
import com.pcmsolutions.device.MidiDeviceMarshall;
import com.pcmsolutions.gui.FlashMsg;
import com.pcmsolutions.gui.ZoeosFrame;
import com.pcmsolutions.license.LicenseKeyManager;
import com.pcmsolutions.system.callback.Callback;
import com.pcmsolutions.system.tasking.*;
import com.pcmsolutions.system.threads.ZDefaultThread;
import sunex.systemkernel.common.SecurityPreferences;

import javax.swing.*;
import java.util.*;

/**
 * User: paulmeehan
 * Date: 09-Aug-2004
 * Time: 09:02:50
 */
class Impl_ZDeviceManager implements ZDeviceManager, ZDeviceListener {
    // DEVICE HANDLING
    private final Vector unidentifiedMessages = new Vector();
    private final Map<ZExternalDevice, ZExternalDevice> duplicateDevices = new Hashtable<ZExternalDevice, ZExternalDevice>();
    private final Map<ZExternalDevice, Object> devices = Collections.synchronizedMap(new LinkedHashMap<ZExternalDevice, Object>());
    //private volatile boolean startBarrier = false;
    private final Vector revokedDevices = new Vector();
    private final Vector listeners = new Vector();
    private final ManageableTicketedQ deviceQ = QueueFactory.createTicketedQueue(this, "device manager Q", 6);

    {
        deviceQ.start();
    }

    public Impl_ZDeviceManager() {
        Zoeos.addZDeviceListener(this);
    }

    public Impl_ZDeviceManager(List startupHuntReplies) {
        processDeviceHunt(startupHuntReplies);
    }

    public void performHunt() {
        //Thread t = new Thread(){
        // public void run(){
        ZMidiSystem.DeviceHunter hunter = ZMidiSystem.getDeviceHunter();
        List replies = hunter.hunt(127); // passing all devices ID
        processDeviceHunt(replies);
        //        }
        //        };
        //      t.stateStart();
    }

    public ZExternalDevice getDeviceMatchingIdentityMessageString(String imt) {
        synchronized (devices) {
            Map.Entry<ZExternalDevice, Object> me;
            for (Iterator<Map.Entry<ZExternalDevice, Object>> i = devices.entrySet().iterator(); i.hasNext();) {
                me = (Map.Entry<ZExternalDevice, Object>) i.next();
                if (me.getValue().toString().equals(imt))
                    return me.getKey();
            }
        }
        return null;
    }

    private boolean checkLicensed(LicensedEntity le) {
        return true;
        /*
        int count = 0;
        synchronized (devices) {
            Iterator<ZExternalDevice> i = devices.keySet().iterator();
            while (i.hasNext()) {
                ZExternalDevice d = i.next();
                if (d instanceof LicensedEntity) {
                    if (((LicensedEntity) d).getLicenseProduct().equals(le.getLicenseProduct()) && ((LicensedEntity) d).getLicenseType().equals(le.getLicenseType()))
                        count++;
                }
            }
        }

        // get max quantity for this type
        //int q = LicenseKeyManager.getLoadForType(LicenseKeyManager.demoPrefix + le.getLicenseProduct(), LicenseKeyManager.demoPrefix + le.getLicenseType(), Zoeos.version);
        int q = LicenseKeyManager.getLoadForType(le.getLicenseProduct(), le.getLicenseType(), Zoeos.version);

        if (q == 0 || count >= q)
                */
        /*if (isDemo) {
            try {
                LicenseKeyManager.addLicenseKey(LicenseKeyManager.parseKey(P200083.generateInternalLicense(le.getLicenseProduct(), LicenseKeyManager.demoPrefix + le.getLicenseType(), "DEMO_USER")));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidLicenseKeyException e) {
                e.printStackTrace();
            }
        } else */
       /*
            return false;

        return true;
        */
    }

    private void processDeviceHunt(final List replies) {
        if (replies == null)
            return;
        final MidiDeviceMarshall m = MidiDeviceMarshall.getInstance();

        System.out.println(Zoeos.getZoeosTime() + ": " + "DEVICE HUNT REPLIES: " + replies.size());

        try {
            Zoeos.getInstance().getSystemQ().getPostableTicket(new TicketRunnable() {
                public void run() throws Exception {
                    int num = replies.size();
                    int duped = 0;
                    if (num == 0)
                        new FlashMsg(ZoeosFrame.getInstance(), 2500, 500, FlashMsg.colorWarning, "NO DEVICES FOUND DURING HUNT");
                    else {
                        List unlicensed = new ArrayList();
                        for (Iterator i = replies.iterator(); i.hasNext();) {
                            final SysexTransactionRecord str = (SysexTransactionRecord) i.next();
                            ZExternalDevice d = m.tryIdentify(str);
                            if (d != null) {
                                ZExternalDevice dup = getDuplicate(d.getDeviceIdentityMessage());
                                if (dup == null) {
                                    if (!Zoeos.isUnlicensed() && d instanceof LicensedEntity && !checkLicensed((LicensedEntity) d)) {
                                        continue;
                                    } else if (Zoeos.isUnlicensed() && SecurityPreferences.expired()) {
                                        unlicensed.add(d);
                                        continue;
                                    }

                                    devices.put(d, d.getDeviceIdentityMessage());
                                    //serializeDeviceMarshalling.setChecked(sun.getBoolean(Zoeos.PREF_serializeDeviceMarshalling, true));

                                    if (!ZoeosPreferences.ZPREF_stopHuntAtPending.getValue()) {
                                        //if (startBarrier == false)
                                        taskStartDevice(d);
                                    } else {
                                        Impl_ZDeviceManager.this.firePendingListChanged();
                                        Impl_ZDeviceManager.this.fireStoppedListChanged();
                                        Impl_ZDeviceManager.this.fireStartedListChanged();
                                        ZoeosFrame.getInstance().showDeviceManager();
                                    }
                                } else {
                                    duped++;
                                    duplicateDevices.put(d, dup);
                                    Impl_ZDeviceManager.this.fireDuplicateListChanged();
                                    System.out.println(Zoeos.getZoeosTime() + ": " + "DUPLICATE MIDI DEVICE: " + str.getReply());
                                }
                            } else {
                                unidentifiedMessages.add(str.getReply());
                                Impl_ZDeviceManager.this.fireUnidentifiedListChanged();
                                System.out.println(Zoeos.getZoeosTime() + ": " + "UNRESOLVED MIDI DEVICE: " + str.getReply());
                            }
                        }
                        
                        if (unlicensed.size() > 0) {
                            System.out.println(unlicensed.size() + (unlicensed.size() == 1 ? " device was" : " devices were") + " not marshalled due to insufficient licensing");
                            new FlashMsg(ZoeosFrame.getInstance(), 10000, 500, FlashMsg.colorWarning, "Evaluation Expired");

                            ZUtilities.zDisposeCollection(unlicensed);
                            unlicensed.clear();
                        }
                        
                        if (duped == num)
                            new FlashMsg(ZoeosFrame.getInstance(), 2500, 500, FlashMsg.colorWarning, "NO DEVICES MARSHALLED FROM HUNT");
                    }
                }
            }, "processDeviceHunt").post(new Callback() {
                public void result(Exception e, boolean wasCancelled) {
                }
            });
        } catch (ResourceUnavailableException e1) {
            e1.printStackTrace();
        }
    }

    public boolean isDuplicate(Object deviceIdentityMessage) {
        assertDevices();
        synchronized (devices) {
            if (devices.containsValue(deviceIdentityMessage))
                return true;
            return false;
        }
    }

    private ZExternalDevice getDuplicate(Object deviceIndentityMessage) {
        assertDevices();
        synchronized (devices) {
            Iterator<ZExternalDevice> i = devices.keySet().iterator();
            while (i.hasNext()) {
                ZExternalDevice next = i.next();
                if (deviceIndentityMessage.equals(devices.get(next)))
                    return (ZExternalDevice) next;
            }

            return null;
        }
    }

    private void assertDevices() {
        synchronized (devices) {
            Iterator<ZExternalDevice> i = devices.keySet().iterator();
            while (i.hasNext()) {
                ZExternalDevice next = i.next();
                if ((next.getState() == ZExternalDevice.STATE_REMOVED))
                    devices.remove(next);
            }
        }
    }

    public Ticket startDevice(final ZExternalDevice d) {
        return deviceQ.getTicket(new TicketRunnable() {
            public void run() throws Exception {
                taskStartDevice(d);
            }
        }, "startDevice");
    }

    private void taskStartDevice(ZExternalDevice d) {
        try {
            if (ZoeosPreferences.ZPREF_serializeDeviceMarshalling.getValue()) {
                synchronized (Impl_ZDeviceManager.this) {
                    SyncTicket t = d.startDevice();
                    t.sync();
                }
            } else
                d.startDevice();
        } catch (ZDeviceStartupException e) {
            ZoeosFrame.getInstance().showDeviceManager();
            e.printStackTrace();
        } catch (IllegalStateTransitionException e) {
            ZoeosFrame.getInstance().showDeviceManager();
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            ZoeosFrame.getInstance().showDeviceManager();
        }
    }

    public Ticket stopDevice(final ZExternalDevice d, final String reason) {
        return deviceQ.getTicket(new TicketRunnable() {
            public void run() throws Exception {
                taskStopDevice(d, reason);
            }
        }, "stopDevice");
    }

    private void taskStopDevice(ZExternalDevice d, String reason) {
        try {
            d.stopDevice(false, reason);
        } catch (IllegalStateTransitionException e) {
            e.printStackTrace();
        }
    }

    public Ticket removeDevice(final ZExternalDevice d, final boolean saveState) {
        return deviceQ.getTicket(new TicketRunnable() {
            public void run() throws Exception {
                taskRemoveDevice(d, saveState);
            }
        }, "removeDevice");
    }

    private void taskRemoveDevice(ZExternalDevice d, boolean saveState) {
        try {
            d.removeDevice(saveState);
        } catch (ZDeviceCannotBeRemovedException e) {
            e.printStackTrace();
        } catch (IllegalStateTransitionException e) {
            e.printStackTrace();
        }
    }

    public Ticket revokeDevices(final String reason) {
        return deviceQ.getTicket(new TicketRunnable() {
            public void run() throws Exception {
                taskRevokeDevices(reason);
            }
        }, "revokeDevices");
    }

    void taskRevokeDevices(String reason) {
        synchronized (revokedDevices) {
            List l = Impl_ZDeviceManager.this.getRunningList();
            int num = l.size();
            ZExternalDevice dev;
            for (int n = num - 1; n >= 0; n--) {
                dev = (ZExternalDevice) l.get(n);
                try {
                    dev.stopDevice(true, (reason == null ? "Midi Revocation" : reason));
                    revokedDevices.add(dev);
                } catch (IllegalStateTransitionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Ticket unrevokeDevices() {
        return deviceQ.getTicket(new TicketRunnable() {
            public void run() throws Exception {
                taskUnrevokeDevices();
            }
        }, "revokeDevices");
    }

    private void taskUnrevokeDevices() {
        synchronized (revokedDevices) {
            int num = revokedDevices.size();
            for (int n = 0; n < num; n++) {
                final ZExternalDevice d = (ZExternalDevice) revokedDevices.get(n);
                try {
                    SyncTicket t = d.startDevice();
                    t.sync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            revokedDevices.clear();
        }
    }

    public void clearUnidentified() {
        unidentifiedMessages.clear();
        fireUnidentifiedListChanged();
    }

    public Map<ZExternalDevice, ZExternalDevice> getDuplicateMap() {
        synchronized (duplicateDevices) {
            return new HashMap<ZExternalDevice, ZExternalDevice>(duplicateDevices);
        }
    }

    public void clearDuplicates() {
        Thread t = new ZDefaultThread() {
            public void runBody() {
                synchronized (duplicateDevices) {
                    ZUtilities.zDisposeCollection(duplicateDevices.keySet());
                    duplicateDevices.clear();
                }
                fireDuplicateListChanged();
            }
        };
        t.start();
    }

    public List getPendingList() {
        return getDeviceList(ZExternalDevice.STATE_PENDING);
    }

    public List<ZExternalDevice> getDeviceList(int state) {
        ArrayList<ZExternalDevice> list = new ArrayList<ZExternalDevice>();
        synchronized (devices) {
            ZExternalDevice[] devs = devices.keySet().toArray(new ZExternalDevice[devices.size()]);
            for (ZExternalDevice d : devs) {
                if (d.getState() == state)
                    list.add(d);
            }
        }
        return list;
    }

    public List getRunningList() {
        return getDeviceList(ZExternalDevice.STATE_RUNNING);
    }

    public List getStoppedList() {
        return getDeviceList(ZExternalDevice.STATE_STOPPED);
    }

    public List getUnidentifiedList() {
        return (List) unidentifiedMessages.clone();

    }

    public void addDeviceManagerListener(ZDeviceManagerListener zdml) {
        listeners.add(zdml);
    }

    public void removeDeviceManagerListener(ZDeviceManagerListener zdml) {
        listeners.remove(zdml);
    }

    public void firePendingListChanged() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized (listeners) {
                    int size = listeners.size();
                    for (int n = 0; n < size; n++)
                        try {
                            ((ZDeviceManagerListener) listeners.get(n)).pendingListChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        });
    }

    public void fireStartedListChanged() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized (listeners) {
                    int size = listeners.size();
                    for (int n = 0; n < size; n++)
                        try {
                            ((ZDeviceManagerListener) listeners.get(n)).startedListChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        });
    }

    public void fireStoppedListChanged() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized (listeners) {
                    int size = listeners.size();
                    for (int n = 0; n < size; n++)
                        try {
                            ((ZDeviceManagerListener) listeners.get(n)).stoppedListChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        });
    }

    public void fireUnidentifiedListChanged() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized (listeners) {
                    int size = listeners.size();
                    for (int n = 0; n < size; n++)
                        try {
                            ((ZDeviceManagerListener) listeners.get(n)).unidentifiedListChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        });
    }

    public void fireDuplicateListChanged() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized (listeners) {
                    int size = listeners.size();
                    for (int n = 0; n < size; n++)
                        try {
                            ((ZDeviceManagerListener) listeners.get(n)).duplicateListChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        });
    }

    public void deviceStarted(ZDeviceStartedEvent ev) {
        firePendingListChanged();
        fireStoppedListChanged();
        fireStartedListChanged();
    }

    public void deviceStopped(ZDeviceStoppedEvent ev) {
        fireStartedListChanged();
        fireStoppedListChanged();
    }

    public void devicePending(ZDevicePendingEvent ev) {
        firePendingListChanged();
    }

    public void deviceRemoved(ZDeviceRemovedEvent ev) {
        devices.remove(ev.getDevice());
        System.out.println("Device manager removed " + ev.getDevice());
        //ev.getDeviceLock().removeZDeviceListener(this);
        fireStartedListChanged();
        fireStoppedListChanged();
        firePendingListChanged();
    }

    public void zDispose() {
        ZExternalDevice[] da;
        synchronized (devices) {
            da = devices.keySet().toArray(new ZExternalDevice[devices.size()]);
        }
        for (int i = da.length - 1; i >= 0; i--) {
            try {
                if (da[i].getState() != StdStates.STATE_PENDING) {
                    Ticket stop = stopDevice(da[i], "ZoeOS shutdown");
                    stop.send(0);
                    Ticket remove = removeDevice(da[i], true);
                    remove.send(0);
                } else {
                    Ticket remove = removeDevice(da[i], false);
                    remove.send(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
