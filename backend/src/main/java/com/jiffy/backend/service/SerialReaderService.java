package com.jiffy.backend.service;

import com.fazecast.jSerialComm.SerialPort;
import com.jiffy.backend.model.Tag;
import com.jiffy.backend.repository.TagRepository;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDateTime;

@Service
public class SerialReaderService {

    private SerialPort serialPort;
    private InputStream in;
    private final TagRepository tagRepository;
    private StringBuilder buffer = new StringBuilder();

    public SerialReaderService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @PostConstruct
    public void init() {
        try {
            // 1. Find the COM4 port
            SerialPort[] ports = SerialPort.getCommPorts();
            serialPort = null;
            for (SerialPort port : ports) {
                if (port.getSystemPortName().equals("COM4")) {
                    serialPort = port;
                    break;
                }
            }
            if (serialPort == null) {
                System.err.println("COM4 not found!");
                return;
            }

            // 2. Open and configure the port
            serialPort.openPort();
            serialPort.setBaudRate(115200);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(1);
            serialPort.setParity(SerialPort.NO_PARITY);

            // 3. Setup IO and listener
            in = serialPort.getInputStream();
            System.out.println("jSerialComm on COM4 initialized.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handle incoming data
    public void readSerialData() {
        try {
            int available = in.available();
            byte[] chunk = new byte[available];
            in.read(chunk);

            for (byte b : chunk) {
                char c = (char) b;
                if (c == '\n' || c == '\r') {
                    String epc = buffer.toString().trim();
                    buffer.setLength(0);
                    if (!epc.isEmpty()) {
                        Tag tag = new Tag();
                        tag.setEpc(epc);
                        tag.setTimestamp(LocalDateTime.now());
                        tagRepository.save(tag);
                        System.out.println("Saved EPC: " + epc);
                    }
                } else {
                    buffer.append(c);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
