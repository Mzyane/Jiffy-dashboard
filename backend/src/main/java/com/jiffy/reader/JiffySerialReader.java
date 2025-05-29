package com.jiffy.reader;

import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.Arrays;

public class JiffySerialReader {

    private static final String COM_PORT = "COM4";  // adjust as needed
    private static final int HEADER_BYTE = 0x84;     // 0x84 is the new packet start marker

    public static void main(String[] args) {
        SerialPort comPort = SerialPort.getCommPort(COM_PORT);
        comPort.setBaudRate(9600);  // your reader's baud rate

        if (comPort.openPort()) {
            System.out.println("Successfully opened port: " + COM_PORT);
            InputStream inputStream = comPort.getInputStream();

            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        byte[] rawBytes = Arrays.copyOf(buffer, bytesRead);
                        System.out.println("Raw bytes: " + bytesToHex(rawBytes));

                        String epc = extractEPC(rawBytes);
                        if (epc != null) {
                            System.out.println("Extracted EPC: " + epc);
                            sendToBackend(epc);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("Failed to open port.");
        }
    }

    private static String extractEPC(byte[] rawBytes) {
        for (int i = 0; i < rawBytes.length; i++) {
            if ((rawBytes[i] & 0xFF) == HEADER_BYTE) {
                // Found 0x84

                int epcStart = i + 1; // Start extracting EPC immediately after 0x84
                int epcLength = 12;   // Assume EPC length is 12 bytes

                if (epcStart + epcLength <= rawBytes.length) {
                    byte[] epcBytes = Arrays.copyOfRange(rawBytes, epcStart, epcStart + epcLength);
                    return bytesToHexNoSpaces(epcBytes);
                } else {
                    System.out.println("Packet too short to extract full EPC.");
                    return null;
                }
            }
        }
        System.out.println("Header 0x84 not found, skipping packet.");
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private static String bytesToHexNoSpaces(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static void sendToBackend(String epc) {
        if (epc == null || epc.isEmpty()) {
            System.out.println("Error: Missing EPC, cannot send to backend.");
            return;
        }
    
        // Check if the EPC is of valid length (for example, 24 characters for EPC in hexadecimal)
        if (epc.length() != 24) {
            System.out.println("Error: Invalid EPC format. EPC length should be 24 characters.");
            return;
        }
    
        try {
            // Update your backend URL here
            URL url = new URL("http://localhost:8080/your-correct-endpoint");  
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
    
            // Build your JSON body
            String jsonInputString = "{\"epc\":\"" + epc + "\"}";
    
            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
    
            int responseCode = conn.getResponseCode();
            System.out.println("Backend response code: " + responseCode);
    
            // Read backend response body (even on error)
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
    
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println("Backend response body: " + response.toString());
            }
    
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                System.out.println("Successfully sent EPC to backend.");
            } else {
                System.out.println("Failed to send EPC, response code: " + responseCode);
            }
    
            conn.disconnect();
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}