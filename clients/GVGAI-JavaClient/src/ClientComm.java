import com.google.gson.Gson;
import ontology.Game;
import ontology.Avatar;
import ontology.Types;

import java.io.*;
import java.util.Random;

/**
 * Created by Daniel on 04/03/2017.
 */
public class ClientComm {

    public static enum COMM_STATE {
        START, INIT, ACT, ABORT, ENDED, CHOOSE
    }

    /**
     * Reader of the player. Will read the game state from the client.
     */
    public static BufferedReader input;

    /**
     * Writer of the player. Used to pass the action of the player to the server.
     */
    public static BufferedWriter output;

    /**
     * Writer of the player. Used to pass the action of the player to the server.
     */
    public static PrintWriter fileOutput;

    /**
     * Line separator for messages.
     */
    private String lineSep = System.getProperty("line.separator");

    /**
     * Communication state
     */
    public COMM_STATE commState;

    /**
     * Number of games played
     */
    private int numGames;

    /**
     * Game information
     */
    public ontology.Game game;

    /**
     * Avatar information
     */
    public Avatar avatar;

    /**
     * State information
     */
    public SerializableStateObservation sso;


    /**
     * Indicates if the current game is a training game
     */
    private boolean isTraining;

    /**
     * Creates the client.
     */
    public ClientComm() {
        commState = COMM_STATE.START;
        game = new Game();
        avatar = new Avatar();
        numGames = 0;
        isTraining = false;
        sso = new SerializableStateObservation();
    }

    /**
     * Creates communication buffers and starts listening.
     */
    public void start()
    {
        initBuffers();

        // Comment this for testing purposes
        try {
            listen();
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    private void listen() throws IOException {
        String line = "start client";
        writeToFile(line);

        int messageIdx = 0;
        while (line != null) {
            line = input.readLine();

            writeToFile("going to processing");

            processLine(line);
            commState = processCommandLine();

            if(commState == COMM_STATE.START)
            {
                //We can work on some initialization stuff here.
                writeToFile("start done");
                writeToServer("START_DONE");

            }else if(commState == COMM_STATE.INIT)
            {
                //We can work on some initialization stuff here.
                writeToFile("init done");
                writeToServer("INIT_DONE");

            }else if(commState == COMM_STATE.ACT)
            {
                // TODO: 27/03/2017 Daniel: no agent for the moment
                //This is the place to think and return what action to take.
                ElapsedCpuTimer ect = new ElapsedCpuTimer();
                Random r = new Random();
                String rndAction;
                if (r.nextFloat() < 0.5)
                    rndAction = Types.ACTIONS.ACTION_RIGHT.toString();
                else
                    rndAction = Types.ACTIONS.ACTION_LEFT.toString();
                writeToFile("action: " + rndAction + " " + ect.elapsedMillis());
                writeToServer(rndAction);

            }else if(commState == COMM_STATE.CHOOSE)
            {
                //This is the place to pick a level to be played after the initial 2 levels have gone through
                Random r = new Random();
                Integer message = r.nextInt(3);
                writeToServer(message.toString());

            }else if(commState == COMM_STATE.ABORT)
            {
                // TODO: 27/03/2017 Daniel: is the game stopped ?
                //We can study what happened in the game here.
                //For debug, print here game and avatar info:
                game.printToFile(numGames);
                avatar.printToFile(numGames);

                game = new Game();
                avatar = new Avatar();

                writeToFile("game aborted");

                // TODO: 27/03/2017 Daniel:  after stopped, start another game ???
                writeToServer("GAME_DONE_ABORT");
            }else if(commState == COMM_STATE.ENDED)
            {
                // TODO: 27/03/2017 Daniel: is the game stopped ?
                //We can study what happened in the game here.
                //For debug, print here game and avatar info:
                game.printToFile(numGames);
                avatar.printToFile(numGames);

                game = new Game();
                avatar = new Avatar();

                writeToFile("game ended");

                // TODO: 27/03/2017 Daniel:  after stopped, start another game ???
                writeToServer("GAME_DONE_ENDED");
            } else {
                writeToServer("null");
            }

            messageIdx++;
        }
    }


    /**
     * Creates the buffers for pipe communication.
     */
    private void initBuffers() {
        try {
            fileOutput = new PrintWriter(new File("logs/clientLog.txt"), "utf-8");


            input = new BufferedReader(new InputStreamReader(System.in));
            output = new BufferedWriter(new OutputStreamWriter(System.out));

        } catch (Exception e) {
            System.out.println("Exception creating the client process: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Writes a line to the server, adding a line separator at the end.
     * @param line to write
     */
    private void writeToServer(String line) throws IOException
    {
        output.write(line + lineSep);
        output.flush();
    }

    /**
     * Writes a line to the client debug file, adding a line separator at the end.
     * @param line to write
     */
    private void writeToFile(String line) throws IOException{
        fileOutput.write(line + lineSep);
        fileOutput.flush();
    }

    public COMM_STATE processCommandLine() throws IOException {
        if(sso.gameState == SerializableStateObservation.State.START_STATE)
        {
            writeToFile("game is in start state");
            game.remMillis = sso.elapsedTimer;
            return COMM_STATE.START;

        }if(sso.gameState == SerializableStateObservation.State.INIT_STATE)
        {
            writeToFile("game is in init state");
            game.remMillis = sso.elapsedTimer;
            return COMM_STATE.INIT;

        }else if(sso.gameState == SerializableStateObservation.State.ACT_STATE) {
            game.remMillis = sso.elapsedTimer;
            return COMM_STATE.ACT;

        }else if(sso.gameState == SerializableStateObservation.State.CHOOSE_LEVEL) {
            game.remMillis = sso.elapsedTimer;
            return COMM_STATE.CHOOSE;

        }else if(sso.gameState == SerializableStateObservation.State.ABORT_STATE) {
            game.remMillis = sso.elapsedTimer;
            return COMM_STATE.ABORT;

        }else if(sso.gameState == SerializableStateObservation.State.END_STATE) {
            game.remMillis = sso.elapsedTimer;
            return COMM_STATE.ENDED;

        }

        return commState;
    }

    public void processLine(String json) throws IOException{
        writeToFile("initializing gson");
        try {
            //Gson gson = new GsonBuilder().registerTypeAdapterFactory(new ArrayAdapterFactory()).create();
            Gson gson = new Gson();
            // Debug line
            //fileOutput.write(json);

            if (json.equals("START")){
                this.sso.gameState = SerializableStateObservation.State.START_STATE;
                return;
            }

            //writeToFile(json);

            ElapsedCpuTimer cpu = new ElapsedCpuTimer();
            this.sso = gson.fromJson(json, SerializableStateObservation.class);
            writeToFile("gson initialized " + cpu.elapsedMillis());
        } catch (Exception e){
            e.printStackTrace(fileOutput);
        }
//        String data = gson.fromJson(gson, String.class);
//
//        for (String act : availableActions)
//            avatar.actionList.add(act);
//
//        for (String r : avatarResources)
//        {
//            int key = Integer.parseInt(r.split(",")[0]);
//            int val = Integer.parseInt(r.split(",")[1]);
//            avatar.resources.put(key, val);
//        }
    }

}

