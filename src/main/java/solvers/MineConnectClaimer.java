package solvers;

import io.Future;
import io.Move;
import io.River;
import state.GameState;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MineConnectClaimer implements Solver {


    private Move bestChoice = null;

    @Override
    public Move getNextMove(GameState state) {
        Set<River> freeRivers = state.getUnclaimedRivers();
        setBestChoice(Move.claim(state.getMyPunterId(), freeRivers.iterator().next()));
        Set<Integer> mines = state.getMines();
        BitSet ownSites = bitSetFrom(state.getOwnRivers());

        for (int mineS : mines) {
            for (int mineT : mines) {
                if (Thread.currentThread().isInterrupted()) return null;
                if (mineS == mineT) continue;
                if (state.canReach(state.getMyPunterId(), mineS, mineT)) continue;
                List<River> path = state.getShortestOpenRoute(state.getMyPunterId(), mineS, mineT);
                River best = null;
                int score = 0;
                for (River river : path) {
                    if (!river.isClaimed() || (state.areOptionsActive()
                            && state.getRemainingOptions() > 0
                            && river.canOption(state.getMyPunterId()))) {
                        int myScore = 1;
                        if (mines.contains(river.getSource())) myScore += 100;
                        if (mines.contains(river.getTarget())) myScore += 100;
                        if (ownSites.get(river.getSource())) myScore += 60;
                        if (ownSites.get(river.getTarget())) myScore += 60;
                        if (myScore > score) {
                            best = river;
                            score = myScore;
                            setBestChoice(Move.claim(state.getMyPunterId(), river));
                        }
                    }
                }
                if (best != null) return Move.claim(state.getMyPunterId(), best);
            }
        }

        River best = null;
        int bestPoints = 0;
        for (River river : freeRivers) {
            if (Thread.currentThread().isInterrupted()) return null;
            boolean connectedSource = ownSites.get(river.getSource());
            boolean connectedTarget = ownSites.get(river.getTarget());

            //ignore those unconnected to our current rivers
            if (!connectedSource && ! connectedTarget) continue;

            if (connectedSource && connectedTarget) {
                // both sides touch one of ours - if they are already connected, ignore
                if (state.canReach(state.getMyPunterId(), river.getSource(), river.getTarget())) continue;
            }

            //only one side is connected, we consider this one
            int points = state.getPotentialPoints(river);
            if (best == null || points > bestPoints) {
                best = river;
                bestPoints = points;
                setBestChoice(Move.claim(state.getMyPunterId(), river));
            }
        }

        if (best != null) return Move.claim(state.getMyPunterId(), best);
        return Move.claim(state.getMyPunterId(), freeRivers.iterator().next());
    }

    private static BitSet bitSetFrom(Collection<River> edges) {
        BitSet set = new BitSet();
        for (River r : edges) {
            set.set(r.getSource());
            set.set(r.getTarget());
        }
        return set;
    }

    @Override
    public Future[] getFutures(GameState state) {
        return new Future[0];
    }

    @Override
    public String getName() {
        return "Mine Connect Claimer";
    }

    @Override
    public synchronized Move getBestChoice() {
        return bestChoice;
    }

    private synchronized void setBestChoice(Move move) {
        this.bestChoice = move;
    }

}
