/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tournaments;

import ai.core.AI;
import ai.core.AIWithComputationBudget;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import rts.GameState;
import rts.PartiallyObservableGameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.Trace;
import rts.TraceEntry;
import rts.units.UnitTypeTable;
import util.XMLWriter;

/**
 *
 * @author santi
 */
public class RoundRobinTournament {

    public static int TIMEOUT_CHECK_TOLERANCE = 20;

    public static void runTournament(List<AI> AIs,
            List<String> maps,
            int iterations,
            int maxGameLength,
            int timeBudget,
            int iterationsBudget,
            boolean fullObservability,
            boolean selfMatches,
            boolean timeoutCheck,
            boolean runGC,
            UnitTypeTable utt,
            String traceOutputfolder,
            Writer out,
            Writer progress) throws Exception {
        if (progress != null) {
            progress.write("RoundRobinTournament: Starting tournament\n");
        }

        int wins[][] = new int[AIs.size()][AIs.size()];
        int ties[][] = new int[AIs.size()][AIs.size()];
        int AIcrashes[][] = new int[AIs.size()][AIs.size()];
        int AItimeout[][] = new int[AIs.size()][AIs.size()];
        double accumTime[][] = new double[AIs.size()][AIs.size()];

        out.write("RoundRobinTournament\n");
        out.write("AIs\n");
        for (int i = 0; i < AIs.size(); i++) {
            out.write("\t" + AIs.get(i).toString() + "\n");
        }
        out.write("maps\n");
        for (int i = 0; i < maps.size(); i++) {
            out.write("\t" + maps.get(i) + "\n");
        }
        out.write("iterations\t" + iterations + "\n");
        out.write("maxGameLength\t" + maxGameLength + "\n");
        out.write("timeBudget\t" + timeBudget + "\n");
        out.write("iterationsBudget\t" + iterationsBudget + "\n");
        out.write("fullObservability\t" + fullObservability + "\n");
        out.write("timeoutCheck\t" + timeoutCheck + "\n");
        out.write("runGC\t" + runGC + "\n");
        out.write("iteration\tmap\tai1\tai2\ttime\twinner\tcrashed\ttimedout\n");
        out.flush();
        for (int iteration = 0; iteration < iterations; iteration++) {
            for (int map_idx = 0; map_idx < maps.size(); map_idx++) {
                PhysicalGameState pgs = PhysicalGameState.load(maps.get(map_idx), utt);
                for (int ai1_idx = 0; ai1_idx < AIs.size(); ai1_idx++) {
                    for (int ai2_idx = 0; ai2_idx < AIs.size(); ai2_idx++) {
                        if (!selfMatches && ai1_idx == ai2_idx) {
                            continue;
                        }
                        AI ai1 = AIs.get(ai1_idx).clone();
                        AI ai2 = AIs.get(ai2_idx).clone();

                        if (ai1 instanceof AIWithComputationBudget) {
                            ((AIWithComputationBudget) ai1).setTimeBudget(timeBudget);
                            ((AIWithComputationBudget) ai1).setIterationsBudget(iterationsBudget);
                        }
                        if (ai2 instanceof AIWithComputationBudget) {
                            ((AIWithComputationBudget) ai2).setTimeBudget(timeBudget);
                            ((AIWithComputationBudget) ai2).setIterationsBudget(iterationsBudget);
                        }

                        ai1.reset();
                        ai2.reset();

                        GameState gs = new GameState(pgs.clone(), utt);

                        if (progress != null) {
                            progress.write("MATCH UP: " + ai1 + " vs " + ai2);
                        }

                        boolean gameover = false;
                        int crashed = -1;
                        int timedout = -1;
                        Trace trace = null;
                        TraceEntry te;
                        if (traceOutputfolder != null) {
                            trace = new Trace(utt);
                            te = new TraceEntry(gs.getPhysicalGameState().clone(), gs.getTime());
                            trace.addEntry(te);
                        }
                        do {
                            PlayerAction pa1 = null;
                            PlayerAction pa2 = null;
                            long AI1start = 0, AI2start = 0, AI1end = 0, AI2end = 0;
                            if (fullObservability) {
                                if (runGC) {
                                    System.gc();
                                }
                                try {
                                    AI1start = System.currentTimeMillis();
                                    pa1 = ai1.getAction(0, gs);
                                    AI1end = System.currentTimeMillis();
                                } catch (Exception e) {
                                    crashed = 0;
                                    break;
                                }
                                if (runGC) {
                                    System.gc();
                                }
                                try {
                                    AI2start = System.currentTimeMillis();
                                    pa2 = ai2.getAction(1, gs);
                                    AI2end = System.currentTimeMillis();
                                } catch (Exception e) {
                                    crashed = 1;
                                    break;
                                }
                            } else {
                                if (runGC) {
                                    System.gc();
                                }
                                try {
                                    PartiallyObservableGameState po_gs = new PartiallyObservableGameState(gs, 0);
                                    AI1start = System.currentTimeMillis();
                                    pa1 = ai1.getAction(0, po_gs);
                                    AI1end = System.currentTimeMillis();
                                } catch (Exception e) {
                                    crashed = 0;
                                    break;
                                }
                                if (runGC) {
                                    System.gc();
                                }
                                try {
                                    PartiallyObservableGameState po_gs = new PartiallyObservableGameState(gs, 1);
                                    AI2start = System.currentTimeMillis();
                                    pa2 = ai2.getAction(1, po_gs);
                                    AI2end = System.currentTimeMillis();
                                } catch (Exception e) {
                                    crashed = 1;
                                    break;
                                }
                            }
                            if (timeoutCheck) {
                                long AI1time = AI1end - AI1start;
                                long AI2time = AI2end - AI2start;
                                if (AI1time > timeBudget + TIMEOUT_CHECK_TOLERANCE) {
                                    timedout = 0;
                                    break;
                                }
                                if (AI2time > timeBudget + TIMEOUT_CHECK_TOLERANCE) {
                                    timedout = 1;
                                    break;
                                }
                            }
                            if (traceOutputfolder != null && (!pa1.isEmpty() || !pa2.isEmpty())) {
                                te = new TraceEntry(gs.getPhysicalGameState().clone(), gs.getTime());
                                te.addPlayerAction(pa1.clone());
                                te.addPlayerAction(pa2.clone());
                                trace.addEntry(te);
                            }

                            gs.issueSafe(pa1);
                            gs.issueSafe(pa2);
                            gameover = gs.cycle();
                        } while (!gameover
                                && (gs.getTime() < maxGameLength));
                        
                        if (traceOutputfolder != null) {
                            File folder = new File(traceOutputfolder);
                            if (!folder.exists()) folder.mkdirs();
                            te = new TraceEntry(gs.getPhysicalGameState().clone(), gs.getTime());
                            trace.addEntry(te);
                            XMLWriter xml;
                            ZipOutputStream zip = null;
                            String filename = ai1_idx + "-vs-" + ai2_idx + "-" + map_idx + "-" + iteration;
                            filename = filename.replace("/", "");
                            filename = filename.replace(")", "");
                            filename = filename.replace("(", "");
                            filename = traceOutputfolder + "/" + filename;
                            zip = new ZipOutputStream(new FileOutputStream(filename + ".zip"));
                            zip.putNextEntry(new ZipEntry("game.xml"));
                            xml = new XMLWriter(new OutputStreamWriter(zip));
                            trace.toxml(xml);
                            xml.flush();
                            zip.closeEntry();
                            zip.close();
                        }

                        int winner = -1;
                        if (crashed != -1) {
                            winner = 1 - crashed;
                            if (crashed == 0) {
                                AIcrashes[ai1_idx][ai2_idx]++;
                            }
                            if (crashed == 1) {
                                AIcrashes[ai2_idx][ai1_idx]++;
                            }
                        } else if (timedout != -1) {
                            winner = 1 - timedout;
                            if (timedout == 0) {
                                AItimeout[ai1_idx][ai2_idx]++;
                            }
                            if (timedout == 1) {
                                AItimeout[ai2_idx][ai1_idx]++;
                            }
                        } else {
                            winner = gs.winner();
                        }
                        out.write(iteration + "\t" + map_idx + "\t" + ai1_idx + "\t" + ai2_idx + "\t"
                                + gs.getTime() + "\t" + winner + "\t" + crashed + "\t" + timedout + "\n");
                        out.flush();
                        if (progress != null) {
                            progress.write("Winner: " + winner + "  in " + gs.getTime() + " cycles");
                        }
                        if (progress != null) {
                            progress.write(ai1 + " : " + ai1.statisticsString() + "\n");
                        }
                        if (progress != null) {
                            progress.write(ai2 + " : " + ai2.statisticsString() + "\n");
                        }
                        progress.flush();
                        if (winner == -1) {
                            ties[ai1_idx][ai2_idx]++;
                        } else if (winner == 0) {
                            wins[ai1_idx][ai2_idx]++;
                        }
                        accumTime[ai1_idx][ai2_idx] += gs.getTime();
                    }
                }
            }
        }

        out.write("Wins:\n");
        for (int ai1_idx = 0; ai1_idx < AIs.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < AIs.size(); ai2_idx++) {
                out.write(wins[ai1_idx][ai2_idx] + "\t");
            }
            out.write("\n");
        }
        out.write("Ties:\n");
        for (int ai1_idx = 0; ai1_idx < AIs.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < AIs.size(); ai2_idx++) {
                out.write(ties[ai1_idx][ai2_idx] + "\t");
            }
            out.write("\n");
        }
        out.write("Average Game Length:\n");
        for (int ai1_idx = 0; ai1_idx < AIs.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < AIs.size(); ai2_idx++) {
                out.write(accumTime[ai1_idx][ai2_idx] / (maps.size() * iterations) + "\t");
            }
            out.write("\n");
        }
        out.write("AI crashes:\n");
        for (int ai1_idx = 0; ai1_idx < AIs.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < AIs.size(); ai2_idx++) {
                out.write(AIcrashes[ai1_idx][ai2_idx] + "\t");
            }
            out.write("\n");
        }
        out.write("AI timeout:\n");
        for (int ai1_idx = 0; ai1_idx < AIs.size(); ai1_idx++) {
            for (int ai2_idx = 0; ai2_idx < AIs.size(); ai2_idx++) {
                out.write(AItimeout[ai1_idx][ai2_idx] + "\t");
            }
            out.write("\n");
        }
        out.flush();
        if (progress != null) {
            progress.write("RoundRobinTournament: tournament ended\n");
        }
        progress.flush();
    }
}
