package com.github.hebelala.tasking.zookeeper.monitor;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * Stat monitor, will watch the path's create/delete events.
 *
 * @author hebelala
 */
public class StatMonitor extends AbstractMonitor implements Watcher, AsyncCallback.StatCallback {

    private ZooKeeper zk;
    private String path;
    private boolean existing;
    private StatListener statListener;

    public StatMonitor(ZooKeeper zk, String path, boolean initExisting, StatListener statListener) {
        this.zk = zk;
        this.path = path;
        this.existing = initExisting;
        this.statListener = statListener;
        registerWatcher();
    }

    public interface StatListener {

        void created();

        void deleted();

    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        if (closed) {
            return;
        }
        switch (Code.get(rc)) {
            case OK:
                if (!existing) {
                    existing = true;
                    statListener.created();
                }
                break;
            case NONODE:
                if (existing) {
                    existing = false;
                    statListener.deleted();
                }
                break;
            default:
                registerWatcher();
        }
    }

    @Override
    public void process(WatchedEvent event) {
        if (closed) {
            return;
        }
        switch (event.getType()) {
            case NodeCreated:
            case NodeDeleted:
            case NodeDataChanged:
                registerWatcher();
                break;
            default:
        }
    }

    private void registerWatcher() {
        zk.exists(path, this, this, null);
    }

}
