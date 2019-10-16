package il.ac.bgu.cs.bp.bpjs.context.examples.gol;

import com.google.common.collect.Lists;
import il.ac.bgu.cs.bp.bpjs.context.ContextService;
import il.ac.bgu.cs.bp.bpjs.context.examples.gol.gui.GameView;
import il.ac.bgu.cs.bp.bpjs.context.examples.gol.schema.Cell;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.BProgramRunnerListenerAdapter;
import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;

import javax.swing.*;
import java.util.List;

public class Main {
    private static GameView ui;

    void run(String dbPopulationScript, String persistenceUnit) throws InterruptedException {
        System.out.println(">>>>>>>>>>>>>>>>>> Game-of-Life example <<<<<<<<<<<<<<<<<<<");

        ContextService contextService = ContextService.getInstance();
        /*contextService.addEntityManagerCreateHook(em -> {
            Session session = em.unwrap(Session.class);

            session.doWork(connection -> Function.create(connection, "NGB", new Function() {
                @Override
                protected void xFunc() throws SQLException {
                    int i = value_int(0);
                    int j = value_int(1);
                    result(
                            String.format("(SELECT n FROM Cell n WHERE (" +
                                    "(n.i=%1$s-1 AND n.j=%2$s-1 AND n.alive) OR " +
                                    "(n.i=%1$s-1 AND n.j=%2$s AND n.alive) OR " +
                                    "(n.i=%1$s-1 AND n.j=%2$s+1 AND n.alive) OR " +
                                    "(n.i=%1$s AND n.j=%2$s-1 AND n.alive) OR " +
                                    "(n.i=%1$s AND n.j=%2$s+1 AND n.alive) OR " +
                                    "(n.i=%1$s+1 AND n.j=%1$s-1 AND n.alive) OR " +
                                    "(n.i=%1$s+1 AND n.j=%2$s AND n.alive) OR " +
                                    "(n.i=%1$s+1 AND n.j=%2$s+1 AND n.alive) ))", i, j));
                }
            }));
            session.doWork(connection -> Function.create(connection, "NGB_COUNT_OLD", new Function() {
                @Override
                protected void xFunc() throws SQLException {
                    int i = value_int(0);
                    int j = value_int(1);
                    String query = String.format("SELECT COUNT(n) from Cell n WHERE (" +
                            "(n.i=%1$s-1 AND n.j=%2$s-1 AND n.alive) OR " +
                            "(n.i=%1$s-1 AND n.j=%2$s AND n.alive) OR " +
                            "(n.i=%1$s-1 AND n.j=%2$s+1 AND n.alive) OR " +
                            "(n.i=%1$s AND n.j=%2$s-1 AND n.alive) OR " +
                            "(n.i=%1$s AND n.j=%2$s+1 AND n.alive) OR " +
                            "(n.i=%1$s+1 AND n.j=%2$s-1 AND n.alive) OR " +
                            "(n.i=%1$s+1 AND n.j=%2$s AND n.alive) OR " +
                            "(n.i=%1$s+1 AND n.j=%2$s+1 AND n.alive) ))", i, j);
                    System.out.println(query);
                    result(query);
                }
            }));
            session.doWork(connection -> Function.create(connection, "NGB_COUNT", new Function() {
                @Override
                protected void xFunc() throws SQLException {
                    String paramName = value_text(0);
                    String query = String.format("(SELECT COUNT(n) from Cell n WHERE (" +
                            "(n.i=%1$s.i-1  AND n.j=%1$s.j-1 AND n.alive = true) OR " +
                            "(n.i=%1$s.i-1  AND n.j=%1$s.j AND n.alive = true) OR " +
                            "(n.i=%1$s.i-1  AND n.j=%1$s.j+1 AND n.alive = true) OR " +
                            "(n.i=%1$s.i    AND n.j=%1$s.j-1 AND n.alive = true) OR " +
                            "(n.i=%1$s.i    AND n.j=%1$s.j+1 AND n.alive = true) OR " +
                            "(n.i=%1$s.i+1  AND n.j=%1$s.j.j-1 AND n.alive = true) OR " +
                            "(n.i=%1$s.i+1  AND n.j=%1$s.j AND n.alive = true) OR " +
                            "(n.i=%1$s.i+1  AND n.j=%1$s.j+1 AND n.alive = true) ))", paramName);
                    System.out.println(query);
                    result(query);
                }
            }));
        });*/
        contextService.initFromResources(persistenceUnit, dbPopulationScript, "program-with-mating.js");
        BProgram bprog = contextService.getBProgram();
        contextService.addListener(new BProgramRunnerListenerAdapter() {
            private ContextService.AnyNewContextEvent generationEvent = new ContextService.AnyNewContextEvent("Generation");
            private AnyDieEvent dieEventSet = new AnyDieEvent();
            private AnyReproduceEvent reproduceEventSet = new AnyReproduceEvent();

            @Override
            public void eventSelected(BProgram bp, BEvent e) {
                if(e.name.equals("board size")) {
                    SwingUtilities.invokeLater(() -> ui = new GameView(((Double)e.maybeData).intValue(), bprog));
                }
                List<BEvent> events = Lists.newArrayList();
                if(e instanceof ContextService.TransactionEvent) {
                    events.addAll(Lists.newArrayList(((ContextService.TransactionEvent)e).commands));
                } else {
                    events.add(e);
                }

                ContextService.UpdateEvent tickEvent = new ContextService.UpdateEvent("Tick");

                if(e.name.equals("Pattern")) {
                    ui.setPattern((String) e.maybeData);
                } else if (e.equals(tickEvent)) {
                    ui.incGeneration();
                    try {
                        Thread.sleep(1000);
                        bp.enqueueExternalEvent(new BEvent("GUI ready"));
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    /*new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            bp.enqueueExternalEvent(new BEvent("GUI ready"));
                        } catch (InterruptedException ignored) { }
                    }).start();*/
                } else {
                    events.stream().filter(e1 -> dieEventSet.contains(e1)).forEach(e1 -> {
                        Cell cell = (Cell) ((ContextService.UpdateEvent) e1).parameters.get("cell");
                        ui.setCell(cell.i, cell.j, null);
                    });

                    events.stream().filter(e1 -> reproduceEventSet.contains(e1)).forEach(e1 -> {
                        Cell cell = (Cell) ((ContextService.UpdateEvent) e1).parameters.get("cell");
                        ui.setCell(cell.i, cell.j, "");
                    });
                }
            }
        });
        contextService.run();

        // Simulation of external events
        /*Thread.sleep(3000);
        ContextService.getContextInstances("Generation", Generation.class)
                .forEach(cell -> bprog.enqueueExternalEvent(new BEvent("Click", cell)));*/

//        Thread.sleep(6000);
//        contextService.close();
    }


    public static void main(String[] args) throws InterruptedException {
        new Main().run("db_population.js", "ContextDB");
    }
}
