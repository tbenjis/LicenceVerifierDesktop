package verify;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Stopwatch;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Controller{
    @FXML
    private Pane dpane;
    @FXML
    private JFXButton send_request;
    @FXML
    private JFXButton cancel;
    @FXML
    private JFXProgressBar progress_bar;
    @FXML
    private Label processing_text;
    @FXML
    private Label num_records;
    @FXML
    private Label num_valid;
    @FXML
    private Label num_invalid;
    @FXML
    private Label num_expired;
    @FXML
    private Label num_error;
    @FXML
    private Label request_time;
    @FXML
    private JFXButton choose_file;
    @FXML
    private Label file_path;
    @FXML
    private JFXTextField sleep_time;
    @FXML
    private JFXButton close_btn;
    @FXML
    private JFXButton help_btn;
    @FXML
    private Label title_bar_text;
    private static double xOffset = 0;
    private static double yOffset = 0;
    private Stage stage;
    private List<String[]> licence_data = null;
    private String[] HEADERS = {"LicenceNumber", "DateOfBirth", "FullName", "FirstIssue", "ExpiryDate", "Report", "Note"};
    private boolean IS_RUNNING = false;
    private Timer timer;

    //dir to save files
    private String SEP = System.getProperty("file.separator");
    private String DIR_PATH = SEP +  "LicenceApp" + SEP;

    //the data
    private ArrayList<String[]> expired = new ArrayList<>();
    private ArrayList<String[]> valid = new ArrayList<>();
    private ArrayList<String[]> server_error = new ArrayList<>();
    private ArrayList<String[]> invalid = new ArrayList<>();

    private String INVALID_FILENAME = "invalid.csv";
    private String VALID_FILENAME = "valid.csv";
    private String SERVER_FILENAME = "server_error.csv";
    private String EXPIRED_FILENAME = "expired.csv";

    int MINUTES = 60 * 1000;

    //the range to sleep
    private int RANDOM_RANGE_MIN = 50;
    private int RANDOM_RANGE_MAX = 100;
    private int DEFAULT_SLEEP = 2 * MINUTES;
    private Thread licence_thread;


    @FXML
    public void initialize() {
        assert dpane != null : "fx:id=\"dpane\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert send_request != null : "fx:id=\"send_request\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert cancel != null : "fx:id=\"cancel\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert progress_bar != null : "fx:id=\"progress_bar\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert processing_text != null : "fx:id=\"processing_text\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert num_records != null : "fx:id=\"num_records\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert num_valid != null : "fx:id=\"num_valid\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert num_invalid != null : "fx:id=\"num_invalid\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert num_expired != null : "fx:id=\"num_expired\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert num_error != null : "fx:id=\"num_error\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert request_time != null : "fx:id=\"request_time\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert choose_file != null : "fx:id=\"choose_file\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert file_path != null : "fx:id=\"file_path\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert sleep_time != null : "fx:id=\"sleep_time\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert close_btn != null : "fx:id=\"close_btn\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert help_btn != null : "fx:id=\"help_btn\" was not injected: check your FXML file 'LicenceView.fxml'.";
        assert title_bar_text != null : "fx:id=\"title_bar_text\" was not injected: check your FXML file 'LicenceView.fxml'.";
        //let the pane be draggable
        makeDraggable();

        //reset the pane
        clearPane();

        // force the field to be numeric only
        sleep_time.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    sleep_time.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });

        //choose file
        choose_file.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose a CSV File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                clearPane();
                file_path.setText(selectedFile.getPath());
                validateCSV();
            }
        });

        // help
        help_btn.setOnAction(event -> {
            String help = "Selecting a file requires a CSV file containing the DOB (dd/mm/yyyy) \nand Licence Number with headers as shown below.\n" +
            "CSV Format: (LicenceNumber, DateOfBirth) e.g.\n\n" +
                    "LicenceNumber, DateOfBirth\n" +
                    "TE37878476AB, 12/08/1976\n" +
                    "AC734553778C, 23/11/1987\n\n" +
                    "Once completed, you will be able to save the (Valid, Invalid, Expired \nand Server Error) files in CSV.";
            showDialog("Help", help);
        });

        //enable app close
        close_btn.setOnAction(event -> {
            showQuitDialog();
        });
        send_request.setOnAction(event -> {
            //set the time
            if (sleep_time.getText().trim().length() < 1){
                sleep_time.setText(String.valueOf(DEFAULT_SLEEP/MINUTES));
            }

            if(file_path.getText().trim().equals("")){
                showDialog("Select File", "Select a file to start the process.");
            } else if( Integer.parseInt(sleep_time.getText()) > 180){
                showDialog("Sleep Time Long", "The sleep time is too long, use between 1 and 180 mins");
            } else {
                try
                {
                    URL url = new URL("http://www.google.com");
                    URLConnection connection = url.openConnection();
                    connection.connect();

                    showInfoDialog();

                }catch (Exception e){
                    showDialog("No Internet Connection", "You need an Internet connection to start this process.");
                }
            }
        });

        cancel.setOnAction(event -> {
            if(!IS_RUNNING){
                showDialog("No Process", "You haven't started any process yet.");
            }else {
                showCancelDialog();
            }
        });

    }

    /**
     * Start new server request
     */
    private void setStartRequest(){
        clearTexts();
        send_request.setDisable(false);
        cancel.setDisable(false);
        cancel.setText("Cancel");

        //reset lists
        invalid = new ArrayList<>();
        valid = new ArrayList<>();
        server_error = new ArrayList<>();
        expired = new ArrayList<>();

        Stopwatch stopwatch = Stopwatch.createStarted();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(new Runnable(){
                    @Override
                    public void run() {
                        long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                        String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
                        request_time.setText(String.valueOf(hms));
                    }
                });
            }
        }, 0, 1000);

        //add the headers
        expired.add(HEADERS);
        valid.add(HEADERS);
        server_error.add(HEADERS);
        invalid.add(HEADERS);

        SendRequest sr = new SendRequest();

        Task<Boolean> startRequest = new Task<Boolean>() {
            private int valid_keys;
            private String fullname;
            private String exp_date;
            private String firstissue;
            private String note;
            private int server_e = 0, expired_e = 0, valid_e = 0, invalid_e = 0;

            @Override
            protected Boolean call() {
                //sleep time count
                int COUNT_SLEEP = 0;
                if (Integer.parseInt(sleep_time.getText()) != 0){
                    DEFAULT_SLEEP = Integer.parseInt(sleep_time.getText()) * MINUTES;
                }
                // DO YOUR WORK
                IS_RUNNING = true;
                Platform.runLater(() -> Platform.runLater(() ->progress_bar.setVisible(true)));
                int count_progress=0;
                //Read data
                for(String[] row : licence_data){
                    COUNT_SLEEP++;

                    //randomize data for the number of requests made
                    Random r = new Random();
                    int current_dc = r.nextInt((RANDOM_RANGE_MAX - RANDOM_RANGE_MIN) + 1) + RANDOM_RANGE_MIN; //current count

                    //check if current count is greater than the current random and sleep
                    if(COUNT_SLEEP > current_dc){
                        try {
                            int finalCount_progress = count_progress;
                            Platform.runLater(() -> Platform.runLater(() -> processing_text.setText("Processed " + finalCount_progress + " records, sleeping for " + DEFAULT_SLEEP/MINUTES + " mins ...")));
                            COUNT_SLEEP = 0; //restart again
                            Thread.sleep(DEFAULT_SLEEP);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    if(IS_RUNNING) {
                        try {
                            String[] dob = row[1].trim().split("/");
                            //System.out.println("licence: " + row[0] + " day: " + dob[0] + " month: " + dob[1] + " year: " + dob[2]);
                            sr.getRequest(row[0].trim(), dob[0].trim(), dob[1].trim(), dob[2].trim());

                            //get the data
                            valid_keys = sr.getVerified();
                            fullname = sr.getFullname();
                            exp_date = sr.getExpiryDate();
                            firstissue = sr.getFirstIssue();
                            note = sr.getNote();

                        } catch (Exception e) {
                            //store invalid array
                            valid_keys = 0;
                            note = "Error in data or with server";
                        }
                    }else{
                        Platform.runLater(() -> Platform.runLater(() -> processing_text.setText("Process Cancelled")));
                        this.cancel();
                        break;
                    }
                    //method to set progress
                    updateProgress(++count_progress, licence_data.size());
                    int finalCount = count_progress;
                    Platform.runLater(new Runnable(){
                        @Override
                        public void run() {
                            processing_text.setText("Processing " + finalCount + " of " + licence_data.size() + " ...");
                            if(valid_keys == 0){
                                num_error.setText(String.valueOf(++server_e));
                                server_error.add(new String[]{row[0], row[1], fullname, firstissue, exp_date, "SERVER_ERROR", note});
                            }else if(valid_keys == 1){
                                num_invalid.setText(String.valueOf(++invalid_e));
                                invalid.add(new String[]{row[0], row[1], fullname, firstissue, exp_date, "INVALID", note});
                            }else if(valid_keys == 2){
                                num_valid.setText(String.valueOf(++valid_e));
                                valid.add(new String[]{row[0], row[1], fullname, firstissue, exp_date, "VALID", note});
                            }else if(valid_keys == 3){
                                num_expired.setText(String.valueOf(++expired_e));
                                expired.add(new String[]{row[0], row[1], fullname, firstissue, exp_date, "EXPIRED", note});
                            }
                        }
                    });
                }
                stopwatch.stop();
                timer.cancel();
                Platform.runLater(() -> Platform.runLater(() -> showCompleteDialog()));
                return false;
            }
        };

        //Load Value from Task
        progress_bar.progressProperty().bind(startRequest.progressProperty());

        //SetOnSucceeded method
        startRequest.setOnSucceeded(event ->
        {
          processing_text.setText("Completed");
        });

        startRequest.setOnCancelled(event ->
        {
            processing_text.setText("Process Cancelled");
        });

        //start the thread
        licence_thread = new Thread(startRequest);
        licence_thread.setDaemon(true);
        licence_thread.start();
    }
    /**
     * Clear pane
     */
    void clearPane(){
        file_path.setText("");
        processing_text.setText("");
        progress_bar.setVisible(false);
        num_records.setText("");
        num_error.setText("0");
        num_expired.setText("0");
        num_valid.setText("0");
        num_invalid.setText("0");
        request_time.setText("00:00:00");
    }

    /**
     * Clear pane
     */
    void clearTexts(){
        processing_text.setText("");
        num_error.setText("0");
        num_expired.setText("0");
        num_valid.setText("0");
        num_invalid.setText("0");
        request_time.setText("00:00:00");
    }

    /**
     * Show Cancel dialog on exit
     */
    private void showCancelDialog() {

        JFXDialogLayout dialogContent = new JFXDialogLayout();
        dialogContent.setHeading(new Text("Stop Process"));
        dialogContent.setBody(new Text("Are you sure you want to stop the process?"));
        JFXButton yes = new JFXButton("YES");
        JFXButton no = new JFXButton("NO");
        yes.setStyle("-fx-text-fill: #ef5350;");
        no.setStyle("-fx-text-fill: #0f9d58;");
        dialogContent.setActions(yes, no);
        JFXDialog dialog = new JFXDialog((StackPane) dpane.getScene().getRoot(), dialogContent, JFXDialog.DialogTransition.CENTER);
        dialog.setOverlayClose(false);
        yes.setOnAction(__ -> {
            dialog.close();
            IS_RUNNING = false;
            send_request.setDisable(true);
            cancel.setDisable(true);
            cancel.setText("Canceling...");
            processing_text.setText("Please wait till the current process completes.");
            licence_thread.interrupt();
        });
        no.setOnAction(__ -> {
            dialog.close();
        });
        dialog.show();

    }
    /**
     * Show Quit dialog on exit
     */
    private void showQuitDialog() {

        JFXDialogLayout dialogContent = new JFXDialogLayout();
        dialogContent.setHeading(new Text("Quit Application"));
        dialogContent.setBody(new Text("Are you sure you want to exit the app?"));
        JFXButton yes = new JFXButton("YES");
        JFXButton no = new JFXButton("NO");
        yes.setStyle("-fx-text-fill: #ef5350;");
        no.setStyle("-fx-text-fill: #0f9d58;");
        dialogContent.setActions(yes, no);
        JFXDialog dialog = new JFXDialog((StackPane) dpane.getScene().getRoot(), dialogContent, JFXDialog.DialogTransition.CENTER);
        dialog.setOverlayClose(false);
        yes.setOnAction(__ -> {
            Platform.exit();
        });
        no.setOnAction(__ -> {
            dialog.close();
        });
        dialog.show();

    }

    /**
     * Show proces complete dialog when verification is complete
     */
    private void showCompleteDialog() {
        IS_RUNNING = false;
        send_request.setDisable(false);
        cancel.setDisable(false);
        cancel.setText("Cancel");

        JFXDialogLayout dialogContent = new JFXDialogLayout();
        dialogContent.setHeading(new Text("Process Complete"));
        dialogContent.setBody(new Text("Verification complete. Do you want to save the files?"));
        JFXButton yes = new JFXButton("YES");
        JFXButton no = new JFXButton("NO");
        yes.setStyle("-fx-text-fill: #0f9d58;");
        no.setStyle("-fx-text-fill: #ef5350;");
        dialogContent.setActions(yes, no);
        JFXDialog dialog = new JFXDialog((StackPane) dpane.getScene().getRoot(), dialogContent, JFXDialog.DialogTransition.CENTER);
        dialog.setOverlayClose(false);
        yes.setOnAction(__ -> {
            dialog.close();
            startFileSave();
        });
        no.setOnAction(__ -> {
            dialog.close();
        });
        dialog.show();

    }

    /**
     * Save the CSV files after completion
     */
    private void startFileSave() {

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Directory to Save Report");
        File selectedDirectory = directoryChooser.showDialog(stage);
        if(selectedDirectory != null){
            String SAVE_DIR = selectedDirectory.getAbsolutePath();
            SAVE_DIR = SAVE_DIR + DIR_PATH;

            try {
                File dir = new File(SAVE_DIR);
                dir.mkdirs();

                CSVWriter writer = new CSVWriter(new FileWriter(SAVE_DIR + INVALID_FILENAME, false));
                writer.writeAll(invalid);
                CSVWriter writer1 = new CSVWriter(new FileWriter(SAVE_DIR + VALID_FILENAME, false));
                writer1.writeAll(valid);
                CSVWriter writer2 = new CSVWriter(new FileWriter(SAVE_DIR + SERVER_FILENAME, false));
                writer2.writeAll(server_error);
                CSVWriter writer3 = new CSVWriter(new FileWriter(SAVE_DIR + EXPIRED_FILENAME, false));
                writer3.writeAll(expired);

                showDialog("Finished", "Data extracted to the directory selected.");

                //close files
                writer.close();
                writer1.close();
                writer2.close();
                writer3.close();

            }catch(Exception e){
                e.printStackTrace();
                showDialog("Error", "There was an error writing file. Please try again.");
            }


        }
    }

    /**
     * Show innfo dialog when the user starts the process
     */
    private void showInfoDialog() {

        JFXDialogLayout dialogContent = new JFXDialogLayout();
        dialogContent.setHeading(new Text("How It Works"));
        dialogContent.setBody(new Text("Sleep Time: Sleep after certain amount of requests.\n" +
                "Invalid: Shows Licences that do not exist.\n" +
                "Error: Licences that were unable to be processed due to server error.\n" +
                "Expired: Licences either expired or about to expire in 30 days.\n" +
                "Valid: These are valid licences.\n" +
                "Time: Time it took to process the CSV file in real-time."));
        JFXButton ok = new JFXButton("UNDERSTOOD");
        ok.setStyle("-fx-text-fill: #0f9d58;");
        dialogContent.setActions(ok);
        JFXDialog dialog = new JFXDialog((StackPane) dpane.getScene().getRoot(), dialogContent, JFXDialog.DialogTransition.CENTER);
        dialog.setOverlayClose(false);

        ok.setOnAction(__ -> {
            dialog.close();
            //Start Thread
            if(!IS_RUNNING) {
                setStartRequest();
            }else{
                showDialog("Task Running", "A current task is running, please wait till its finished");
            }
        });
        dialog.show();

    }

    /**
     * Show general dialog
     */
    private void showDialog(String title, String message) {

        JFXDialogLayout dialogContent = new JFXDialogLayout();
        dialogContent.setHeading(new Text(title));
        dialogContent.setBody(new Text(message));
        JFXButton ok = new JFXButton("OK");
        ok.setStyle("-fx-text-fill: #0f9d58;");
        dialogContent.setActions(ok);
        JFXDialog dialog = new JFXDialog((StackPane) dpane.getScene().getRoot(), dialogContent, JFXDialog.DialogTransition.CENTER);
        dialog.setOverlayClose(false);

        ok.setOnAction(__ -> {
            dialog.close();
        });
        dialog.show();
    }



    /**
     * Make the StackPane draggable
     */
    void makeDraggable(){

        dpane.setOnMousePressed(event -> {
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });

        dpane.setOnMouseDragged(event -> {
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        });
    }

    void validateCSV(){
        try {
            //Build reader instance
            CSVReader reader = new CSVReader(new FileReader(file_path.getText()), ',', '"', 1);
            //Read all rows at once
            licence_data = reader.readAll();
            num_records.setText(String.valueOf(licence_data.size()) + " Records");
        } catch (IOException e) {
            e.printStackTrace();
            showDialog("Error", "Error reading CSV file. Please check your file.");
        }
    }


}

