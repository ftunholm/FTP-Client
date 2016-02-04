package com.company;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.IOException;

public class Main extends JFrame {

    protected JTextArea mainTextArea;
    protected JTextField commandField;

    public Main() {
        setTitle("FTP-client");
        setMinimumSize(new Dimension(700, 350));

        mainTextArea = new JTextArea();
        mainTextArea.setBackground(Color.BLACK);
        mainTextArea.setForeground(Color.WHITE);
        mainTextArea.setEditable(false);
        commandField = new JTextField();
        commandField.setBackground(Color.BLACK);
        commandField.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(mainTextArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        DefaultCaret caret = (DefaultCaret) mainTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(commandField, BorderLayout.SOUTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        add(panel);
        setVisible(true);
        pack();
    }
    public static void main(String[] args) throws IOException {
        new Ftp();
    }
}
