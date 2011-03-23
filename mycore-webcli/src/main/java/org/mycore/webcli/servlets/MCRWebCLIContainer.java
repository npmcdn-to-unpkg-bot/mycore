/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.webcli.servlets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.servlet.http.HttpSession;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRExternalCommandInterface;
import org.mycore.webcli.cli.MCRCommandPool;
import org.mycore.webcli.cli.command.MCRAddCommands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Is a wrapper class around command execution.
 * 
 * Commands will be {@link #addCommand(String) queued} and executed in a
 * seperate thread. All logging events in that thread are grabbed and can be
 * retrieved by the {@link #getLogs() getLogs} method.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2.0
 */
class MCRWebCLIContainer {
    Future<Boolean> curFuture;

    private static Map<String, List<MCRCommand>> knownCommands;

    private final ProcessCallable processCallable;

    private static final String JSON_POOL_NAME = "Pooled Commands";

    private static long knownCommandsUpdateTime;

    private static final Logger LOGGER = Logger.getLogger(MCRWebCLIContainer.class);

    private static final ExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "WebCLI");
        }
    });

    /**
     * Will instantiate this container with a list of supported commands.
     * 
     * @param knownCommands
     *            commands that are supported by the web gui.
     * @param session
     *            the current HttpSession of the usere using the gui.
     * 
     */
    public MCRWebCLIContainer(HttpSession session) {
        processCallable = new ProcessCallable(MCRSessionMgr.getCurrentSession(), session);
    }

    /**
     * Adds this <code>cmd</code> to the current command queue.
     * 
     * The thread executing the commands will be started automatically if the
     * queue was previously empty.
     * 
     * @param cmd a valid String representation of a {@link #MCRWebCLIContainer(List, HttpSession) known} <code>MCRCommand</code>
     */
    public void addCommand(String cmd) {
        LOGGER.info("appending command: " + cmd);
        getCommandQueue().add(cmd);
        if (!isRunning()) {
            curFuture = executor.submit(processCallable);
        }
    }

    /**
     * Gets the current command queue.
     * @return the queue of commands yet to be processed
     */
    public LinkedList<String> getCommandQueue() {
        return processCallable.commands;
    }

    /**
     * Returns the status of the command execution thread.
     * @return true if the thread is running
     */
    public boolean isRunning() {
        return !(curFuture == null || curFuture.isDone());
    }

    /**
     * Returns all logs that were grabbed in the command execution thread.
     * 
     * This method is backed by a queue that will be empty after the method returns. 
     * @return
     *  {"logs": {<br/>
     *  &#160;&#160;&#160;&#160;"logLevel": <code>logLevel</code>,<br/>
     *  &#160;&#160;&#160;&#160;"message": <code>message</code>,<br/>
     *  &#160;&#160;&#160;&#160;"exception": <code>exception</code><br/>
     *  }}
     */
    public JsonObject getLogs() {
        JsonObject json = new JsonObject();
        json.add("logs", getJSONLogs(processCallable.logs));
        return json;
    }

    public static JsonObject getKnownCommands() {
        updateKnownCommandsIfNeeded();
        JsonObject commandsJSON = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        commandsJSON.add("commands", jsonArray);
        for (Map.Entry<String, List<MCRCommand>> entry : knownCommands.entrySet()) {
            JsonArray commands = new JsonArray();
            for (final MCRCommand cmd : entry.getValue()) {
                commands.add(new JsonPrimitive(cmd.showSyntax()));
            }
            JsonObject item = new JsonObject();
            item.addProperty("name", entry.getKey());
            item.add("commands", commands);
            jsonArray.add(item);
        }
        return commandsJSON;
    }

    protected static void initializeCommands() {
        if (knownCommands == null) {
            knownCommands = new TreeMap<String, List<MCRCommand>>();
            ArrayList<MCRCommand> basicCommands = new ArrayList<MCRCommand>();
            basicCommands.add(new MCRCommand("process {0}", "org.mycore.webcli.cli.MCRCommandLineInterface.readCommandsFile String",
                "Execute the commands listed in the text file {0}."));
            basicCommands.add(new MCRCommand("show command statistics", "org.mycore.webcli.cli.MCRCommandLineInterface.showCommandStatistics",
                "Show statistics on number of commands processed and execution time needed per command"));
            basicCommands.add(new MCRAddCommands());
            LOGGER.warn("known commands:" + knownCommands);
            knownCommands.put("Basic commands", basicCommands);
            String internalClasses = MCRConfiguration.instance().getString("MCR.CLI.Classes.Internal", "");
            String externalClasses = MCRConfiguration.instance().getString("MCR.CLI.Classes.External", "");
            initializeCommands(knownCommands, internalClasses);
            initializeCommands(knownCommands, externalClasses);
        } else if (knownCommands.containsKey(JSON_POOL_NAME)) {
            knownCommands.remove(JSON_POOL_NAME);
        }
        knownCommandsUpdateTime = MCRCommandPool.instance().getLastModified();
        final ArrayList<MCRCommand> dynamicCommandList = MCRCommandPool.instance().getPossibleCommands();
        if (dynamicCommandList.size() > 0) {
            knownCommands.put(JSON_POOL_NAME, dynamicCommandList);
        }
    }

    private static void updateKnownCommandsIfNeeded() {
        if (knownCommands == null || knownCommandsUpdateTime < MCRCommandPool.instance().getLastModified())
            initializeCommands();
    }

    private static void initializeCommands(Map<String, List<MCRCommand>> knownCommands, String commandClasses) {
        for (StringTokenizer st = new StringTokenizer(commandClasses, ","); st.hasMoreTokens();) {
            String classname = st.nextToken();
            LOGGER.debug("Loading commands from class " + classname);
            Object obj;
            try {
                obj = Class.forName(classname).newInstance();
            } catch (Exception e) {
                String msg = "Could not instantiate class " + classname;
                throw new org.mycore.common.MCRConfigurationException(msg, e);
            }
            ArrayList<MCRCommand> commands = ((MCRExternalCommandInterface) obj).getPossibleCommands();
            knownCommands.put(obj.getClass().getSimpleName(), commands);
        }
    }

    private static JsonArray getJSONLogs(Queue<LoggingEvent> events) {
        JsonArray array = new JsonArray();
        while (!events.isEmpty()) {
            LoggingEvent event = events.poll();
            JsonObject json = new JsonObject();
            json.addProperty("logLevel", event.getLevel().toString());
            json.addProperty("message", event.getRenderedMessage());
            String exception = null;
            if (event.getThrowableInformation() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                event.getThrowableInformation().getThrowable().printStackTrace(pw);
                pw.close();
                exception = sw.toString();
            }
            json.addProperty("exception", exception);
            json.addProperty("time", event.timeStamp);
            array.add(json);
        }
        return array;
    }

    private static class ProcessCallable implements Callable<Boolean> {

        ConcurrentLinkedQueue<LoggingEvent> logs;

        LinkedList<String> commands;

        HttpSession hsession;

        MCRSession session;

        Log4JGrabber logGrabber;

        public ProcessCallable(MCRSession session, HttpSession hsession) {
            this.commands = new LinkedList<String>();
            this.session = session;
            this.hsession = hsession;
            this.logs = new ConcurrentLinkedQueue<LoggingEvent>();
            this.logGrabber = new Log4JGrabber();
        }

        public Boolean call() throws Exception {
            return processCommands(true);
        }

        /**
         * method mainly copied from CLI class
         * 
         * @param command
         * @return true if command processed successfully
         * @throws IOException
         */
        private boolean processCommand(String command, boolean continueIfOneFails) throws IOException {
            LOGGER.info("Processing command:'" + command + "' (" + commands.size() + " left)");
            long start = System.currentTimeMillis();
            session.beginTransaction();
            try {
                List<String> commandsReturned = null;
                for (List<MCRCommand> cmds : knownCommands.values()) {
                    //previous attempt to run command was successful
                    if (commandsReturned != null)
                        break;
                    commandsReturned = runCommand(command, cmds);
                }
                updateKnownCommandsIfNeeded();
                session.commitTransaction();
                if (commandsReturned != null)
                    LOGGER.info("Command processed (" + (System.currentTimeMillis() - start) + " ms)");
                else {
                    throw new MCRUsageException("Command not understood: " + command);
                }
            } catch (Exception ex) {
                LOGGER.error("Command '" + command + "' failed. Performing transaction rollback...", ex);
                try {
                    session.rollbackTransaction();
                } catch (Exception ex2) {
                    LOGGER.error("Error while perfoming rollback for command '" + command + "'!", ex2);
                }
                if (!continueIfOneFails) {
                    saveQueue(command);
                }
                return false;
            } finally {
                session.beginTransaction();
                MCRHIBConnection.instance().getSession().clear();
                session.commitTransaction();
            }
            return true;
        }

        private List<String> runCommand(String command, List<MCRCommand> commandList) throws IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException {
            List<String> commandsReturned = null;
            for (MCRCommand currentCommand : commandList) {
                commandsReturned = currentCommand.invoke(command, this.getClass().getClassLoader());
                if (commandsReturned != null) // Command was executed
                {
                    // Add commands to queue
                    if (commandsReturned.size() > 0) {
                        LOGGER.info("Queueing " + commandsReturned.size() + " commands to process");
                        commands.addAll(0, commandsReturned);
                    }

                    break;
                }
            }
            return commandsReturned;
        }

        protected void saveQueue(String lastCommand) throws IOException {
            // lastCommand is null if work is not stopped at first error
            if (lastCommand == null) {
                LOGGER.error("Some commands failed.", null);
            } else {
                LOGGER.error("The following command failed: '" + lastCommand + "'", null);
            }
            if (!commands.isEmpty())
                LOGGER.info("There are " + commands.size() + " other commands still unprocessed.");

            File file = new File(MCRWebCLIServlet.class.getSimpleName() + "-unprocessed-commands.txt");
            LOGGER.info("Writing unprocessed commands to file " + file.getAbsolutePath());

            try {
                PrintWriter pw = new PrintWriter(new FileWriter(file));
                if (lastCommand != null) {
                    pw.println(lastCommand);
                }
                for (String command : commands)
                    pw.println(command);
                pw.close();
            } catch (IOException ex) {
                LOGGER.error("Cannot write to " + file.getAbsolutePath(), ex);
            }
        }

        protected boolean processCommands(boolean continueIfOneFailes) throws IOException {
            LinkedList<String> failedQueue = new LinkedList<String>();
            int maxSessionTime = hsession.getMaxInactiveInterval();
            logGrabber.grabCurrentThread();
            logGrabber.setLogEventList(logs);
            // start grabbing logs of this thread
            Logger.getRootLogger().addAppender(logGrabber);
            // register session to MCRSessionMgr
            MCRSessionMgr.setCurrentSession(session);
            try {
                // don't let session expire
                hsession.setMaxInactiveInterval(-1);
                while (!commands.isEmpty()) {
                    String command = commands.poll();
                    if (!processCommand(command, continueIfOneFailes)) {
                        if (!continueIfOneFailes) {
                            return false;
                        }
                        failedQueue.add(command);
                    }
                }
                if (failedQueue.isEmpty()) {
                    return true;
                } else {
                    saveQueue(null);
                    return false;
                }
            } finally {
                // restore old session expire time
                hsession.setMaxInactiveInterval(maxSessionTime);
                // stop grabbing logs of this thread
                Logger.getRootLogger().removeAppender(logGrabber);
                // release session
                MCRSessionMgr.releaseCurrentSession();
            }
        }

    }

    private static class Log4JGrabber extends AppenderSkeleton {

        public String webCLIThread;

        public Queue<LoggingEvent> logEvents;

        public Log4JGrabber() {
            grabCurrentThread();
        }

        public void grabCurrentThread() {
            this.webCLIThread = Thread.currentThread().getName();
        }

        public void setLogEventList(Queue<LoggingEvent> logs) {
            logEvents = logs;
        }

        @Override
        protected void append(LoggingEvent e) {
            if (webCLIThread.equals(e.getThreadName())) {
                logEvents.add(e);
            }
        }

        public void close() {
        }

        public boolean requiresLayout() {
            return false;
        }
    }
}
