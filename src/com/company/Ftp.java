package com.company;

import com.google.common.io.CountingInputStream;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.Socket;

/**
 * Created by LogiX on 2016-02-04.
 */
public class Ftp extends Main implements KeyListener {

    private BufferedReader in;
    private BufferedWriter out;
    private CountingInputStream dataIn;
    private BufferedReader passiveIn;
    private Socket socket;
    private String username = "anonymous";
    private String password = "";
    private int dataSocketPort;

    public Ftp() throws IOException {
        commandField.addKeyListener(this);
        commandField.requestFocus();
        connect();
    }

    private void connect() {
        try {
            socket = new Socket("ftp.linkura.se", 21);
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            if (socket.isConnected()) {
                reader.start();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    Thread reader = new Thread(new Runnable() {
        @Override
        public void run() {
            String line;
            while (true) {
                try {
                    line = in.readLine();
                    handleInput(line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    private int getPercent(long a, long b) {
        return (int)((float)a / (float)b * 100);
    }
    private double byteToMB(double x) {
        return Math.round((x / (1024 * 1024)) * 10d) / 10d;
    }
    private double getDownloadRate(long startTime, long bytesRead) {
        double elapsedTime = ((double)System.nanoTime() - (double)startTime)/1000000000;
        double result = ((double)bytesRead/1024)/elapsedTime;
        return Math.round(result * 10d)/10d;
    }

    private void createDataThread(final String filename, final int bytes) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream fout;
                long startTime = System.nanoTime();
                try {
                    fout = new FileOutputStream(filename);
                    int in;
                    int counter = 0;
                    while ((in = dataIn.read()) != -1) {
                        counter++;
                        if (counter % 100 == 0 || dataIn.getCount() == bytes) {
                            setTitle("FTP-client - " + getPercent(dataIn.getCount(), bytes) + " %" +
                                    "  (" + byteToMB((double) dataIn.getCount()) + "mb / " + byteToMB((double) bytes) + "mb)" +
                            "  kb/s: " + getDownloadRate(startTime, dataIn.getCount()));
                        }
                        fout.write(in);
                    }
                    setTitle("FTP-client");
                    mainTextArea.append("\n");
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void readPassiveInput() throws IOException {
        try {
            Socket dataSocket = new Socket("ftp.linkura.se", dataSocketPort);
            passiveIn = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
            String line;
            while ((line = passiveIn.readLine()) != null) {
                handleInput(line);
            }
            passiveIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleInput(String input) throws IOException {
        mainTextArea.append(input + "\n");

        if (input.startsWith("220")) { //start
            write("USER " + username);
        }
        else if (input.startsWith("530")) { //login with user and pass
            write("USER " + username);
        }
        else if (input.startsWith("331")) { //specify password
            write("PASS " + password);
        }
        else if (input.startsWith("230")) { //login success
            mainTextArea.append("Command list:\n\n");
            mainTextArea.append("cd [Directory] - Change directory\nls - List files in directory\nget [filename] - Downloads the specified file\n\n");
            write("PASV");
        }
        else if (input.startsWith("227")) { //entring passive mode
            int start = input.indexOf("(");
            int end = input.indexOf(")");
            String[] ports = input.substring(start, end).split(",");
            dataSocketPort = Integer.parseInt(ports[ports.length-2])*256+Integer.parseInt(ports[ports.length-1]);
        }
        else if (input.startsWith("500")) { //Unknown command
            write("PASV");
        }
        else if (input.startsWith("550")) { //Requested action not taken.
            write("PASV");
        }
        else if (input.startsWith("226")) { //Directory send ok or file transfer complete
            write("PASV");
        }
        else if (input.startsWith("150 Opening BINARY")) {
            String[] parts = input.split(" ");
            String filename = parts[parts.length-3].trim();
            int bytes = Integer.parseInt(parts[parts.length - 2].replace("(", "").trim());
            createDataThread(filename, bytes);
        }
    }

    private void handleOutput(String command) throws IOException {
        if (command.startsWith("ls")) {
            write("LIST");
            readPassiveInput();
        }
        else if (command.startsWith("PASV")) {
            write(command);
        }
        else if (command.startsWith("get")) {
            Socket dataSocket = new Socket("ftp.linkura.se", dataSocketPort);
            dataIn = new CountingInputStream(dataSocket.getInputStream());
            write("RETR " + command.replace("get", "").trim());
        }
        else if (command.startsWith("cd")) {
            write("CWD " + command.replace("cd", "").trim());
        }
        else if (command.startsWith("cd ..")) {
            write("CDUP");
        }
        else {
            write(command);
        }
    }

    private void write(String output) throws IOException {
        out.write(output + "\r\n");
        out.flush();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            try {
                handleOutput(commandField.getText());
                commandField.setText("");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
