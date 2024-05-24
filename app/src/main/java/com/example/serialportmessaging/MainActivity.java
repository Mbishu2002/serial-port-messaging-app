package com.example.serialportmessaging;

import android.content.Context;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView messageListView;
    private EditText messageEditText;
    private Button sendButton;
    private List<String> messageList;
    private ArrayAdapter<String> messageAdapter;

    private UsbSerialPort serialPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageListView = findViewById(R.id.messageListView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        messageList = new ArrayList<>();
        messageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messageList);
        messageListView.setAdapter(messageAdapter);

        // Initialize USB Serial Port
        initSerialPort();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString();
                if (!message.isEmpty()) {
                    sendMessage(message);
                    messageEditText.setText("");
                }
            }
        });
    }

    private void initSerialPort() {
        // Find all available drivers from attached devices.
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();
        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null) {
            return;
        }

        // Get the first port of the USB device.
        serialPort = driver.getPorts().get(0);
        try {
            serialPort.open(connection);
            serialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            // Listen for incoming data
            receiveMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            serialPort.write(message.getBytes(), 1000);
            addMessage("Sent: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void receiveMessage() {
        serialPort.read(new UsbSerialPort.ReadCallback() {
            @Override
            public void onReceivedData(byte[] data) {
                final String receivedMessage = new String(data, Charset.forName("UTF-8"));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addMessage("Received: " + receivedMessage);
                    }
                });
            }
        });
    }

    private void addMessage(String message) {
        messageList.add(message);
        messageAdapter.notifyDataSetChanged();
    }
}
