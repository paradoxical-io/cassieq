package io.paradoxical.cassieq.workers;

import io.paradoxical.cassieq.ServiceConfiguration;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.RepairWorkerFactory;
import io.paradoxical.cassieq.model.QueueDefinition;
import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import org.jgroups.JChannel;
import org.jgroups.protocols.raft.RAFT;
import org.jgroups.protocols.raft.Role;
import org.jgroups.raft.RaftHandle;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class LeaderBasedRepairWorker implements RAFT.RoleChange, RepairWorkerManager {

    private static final Logger logger = getLogger(LeaderBasedRepairWorker.class);

    private static int MEMBER_ID_LAST = 0;

    private final QueueRepository queueRepo;
    private RepairWorkerFactory repairWorkerFactory;
    private RaftHandle raftHandle;

    @Inject
    public LeaderBasedRepairWorker(ServiceConfiguration config, JChannel jChannel, QueueRepository queueRepo, RepairWorkerFactory repairWorkerFactory) throws Exception {
        raftHandle = new RaftHandle(jChannel, null);
        String raftId = String.valueOf(++MEMBER_ID_LAST);
        raftHandle.raftId(config.getServerConf().getName() + "_" + raftId);
        jChannel.connect("raft-cluster");

        this.queueRepo = queueRepo;
        this.repairWorkerFactory = repairWorkerFactory;
    }

    protected void startAll() {
        for (QueueDefinition queueDefinition : queueRepo.getQueues()) {
            repairWorkerFactory.forQueue(queueDefinition).start();
        }
    }

    protected void stopAll() {
        for (QueueDefinition queueDefinition : queueRepo.getQueues()) {

            repairWorkerFactory.forQueue(queueDefinition).stop();
        }
    }

    @Override
    public void roleChanged(Role role) {
        logger.info("ROLE_CHANGED: " + raftHandle.raftId() + " became " + role);

        if (role == Role.Leader) {
            startAll();
        }
        else {
            stopAll();
        }
    }

    @Override
    public void start() {
        raftHandle.addRoleListener(this);
        if (raftHandle.isLeader()) {
            roleChanged(Role.Leader);
        }
    }

    @Override
    public void stop() {
        raftHandle.removeRoleListener(this);
        if (raftHandle.isLeader()) {
            roleChanged(Role.Follower);
        }
    }

    public boolean isLeader() {
        return raftHandle.isLeader();
    }
}