package com.github.hebelala.tasking.zookeeper.monitor;

import org.apache.zookeeper.*;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;

import java.util.Arrays;

/**
 * Data monitor, will watch the path's create/change/delete events. The zk client must has the READ permission with the path.
 *
 * @author hebelala
 */
public class DataMonitor extends AbstractMonitor implements Watcher, AsyncCallback.StatCallback, AsyncCallback.DataCallback {

    private ZooKeeper zk;
    private String path;
    private byte[] preData;
    private DataListener dataListener;

    public DataMonitor(ZooKeeper zk, String path, byte[] initData, DataListener dataListener) {
        this.zk = zk;
        this.path = path;
        this.preData = initData;
        this.dataListener = dataListener;
        registerExistsWatcher();
    }

    public interface DataListener {

        void changed(byte[] data);

    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        if (closed) {
            return;
        }
        byte[] data = null;
        switch (Code.get(rc)) {
            case OK:
                try {
                    data = zk.getData(path, false, null);
                } catch (KeeperException e) {
                    // We don't need to worry about recovering now.
                    // The watch callbacks will kick off any exception handling.
                } catch (InterruptedException e) {
                    return;
                }
            case NONODE:
                if (!Arrays.equals(preData, data)) {
                    dataListener.changed(data);
                    preData = data;
                }
                break;
            default:
                registerExistsWatcher();
        }
    }

    @Override
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
        if (closed) {
            return;
        }
        switch (Code.get(rc)) {
            case OK:
                if (!Arrays.equals(preData, data)) {
                    dataListener.changed(data);
                    preData = data;
                }
                break;
            case NONODE:
                if (!Arrays.equals(preData, data)) {
                    dataListener.changed(data);
                    preData = data;
                }
                registerExistsWatcher();
                break;
            default:
                registerGetDataWatcher();
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (closed) {
            return;
        }
        switch (event.getType()) {
            case NodeCreated:
            case NodeDataChanged:
                registerGetDataWatcher();
                break;
            case NodeDeleted:
                registerExistsWatcher();
                break;
            default:
        }
    }

    private void registerExistsWatcher() {
        zk.exists(path, this, this, null);
    }

    private void registerGetDataWatcher() {
        zk.getData(path, this, this, null);
    }

}
