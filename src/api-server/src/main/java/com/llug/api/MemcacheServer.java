package com.llug.api;

import java.util.HashMap;
import java.util.Map;

public class MemcacheServer {
    Map<String, byte[]> dataMap = new HashMap<String, byte[]>();
    
    
    void parseIncomingCommandString(byte[] commandBytes) {
        boolean hasMoreCommands = true;
        int begin = 0;
        byte cr = (byte) '\r';
        byte lf = (byte) '\n';
        
        while (hasMoreCommands) {
            // determine the incoming command
            int crlfPosn = begin;
            boolean found = false;
            
            // assume we don't have more commands
            hasMoreCommands = false;
            
            // searches for the next \r\n sequence
            while (crlfPosn < commandBytes.length - 1) {
                if (commandBytes[crlfPosn] == cr && commandBytes[crlfPosn + 1] == lf) {
                    found = true;
                    break;
                } else {
                    crlfPosn++;
                }
            }
            
            // did we find \r\n?
            if (found) {
                // get the cmd protocol string:
                byte[] cmdBytes = new byte[crlfPosn - begin];
                System.arraycopy(commandBytes, begin, cmdBytes, 0, crlfPosn - begin);

                String cmd = new String(cmdBytes);
                String[] cmdParts = cmd.split(" ");
                
                // todo: bounds check
                if (cmdParts[0].equals("get")) {
                    // todo:  turn into a nice function
                    // todo bounds checking
                    String key = cmdParts[1];
                    
                    byte[] obj = dataMap.get(key);
                    
                    begin = crlfPosn + 2;
                    hasMoreCommands = true;
                } else if (cmdParts[0].equals("set")) {
                    if (cmdParts.length ==  5) {
                        String set = cmdParts[0];
                        String key = cmdParts[1];

                        // nb, technically only 16-32 bits max.  Long allows for parsing as unsigned 32 bits
                        Long flags = Long.parseLong(cmdParts[2]);
                        Long expTime = Long.parseLong(cmdParts[3]);
                        Long payloadLength = Long.parseLong(cmdParts[4]);
                        
                        // good, now verify that the payload length is correct.  -2 to account for crlf separating command from payload
                        if (commandBytes.length - crlfPosn - 2 >= payloadLength) {
                            byte[] payloadBytes = new byte[payloadLength.intValue()];
                            System.arraycopy(commandBytes, crlfPosn + 2, payloadBytes, 0, payloadLength.intValue());
                            
                            // now we have the bytes, set it
                            dataMap.put(key, payloadBytes);
                            
                            begin = crlfPosn + 2 + payloadLength.intValue()  + 2;
                            
                            hasMoreCommands = true;
                        }
                    }
                }
            }
        }
    }
    
    
    byte[] get(byte[] commandBytes, int begin, int end) {
        // protocol defined https://github.com/memcached/memcached/blob/master/doc/protocol.txt,
        // is straight text, so we can assume straight conversion from bytes to string
        
        String cmd = new String(commandBytes);  // assumes UTF-8
        
        // do some basic parsing to make sure it is in correct format
        if (cmd.startsWith("get ") && cmd.endsWith("\r\n")) {
            // shorten the cmd to get the key
            String[] keys = cmd.trim().split(" ");
            
            // the first key is the actual "get", so ignore that
            String key = keys[1];
            
            
            // returns null if doesn't exist
            return dataMap.get(key);
        } else {
            return null;
        }
    }
    
    void set(byte[] commandBytes, int begin, int end) {
        // line 129 from protocol defined https://github.com/memcached/memcached/blob/master/doc/protocol.txt,
        // first, find the delimiter so that we can parse out the set command.  What follows the delimiter
        // is the raw bytes to set
        byte cr = (byte) '\r';
        byte lf = (byte) '\n';
        byte[] crlf = { cr, lf };
        
        int posn = 0;       // track the iteration
        int start = 0;      // store the actual start of the crlf
        int testStart = 0;  // store the start of crlf, while we're testing
        boolean found = false;
        
        while (posn < commandBytes.length - 1) {
            if (commandBytes[posn] == cr && commandBytes[posn + 1] == lf) {
                found = true;
                break;
            } else {
                posn++;
            }
        }
        
        if (found) {
            // get the cmd protocol string:
            byte[] cmdBytes = new byte[posn];
            System.arraycopy(commandBytes, 0, cmdBytes, 0, posn);
            
            String cmd = new String(cmdBytes);
            String[] cmdParts = cmd.split(" ");
            
            if (cmdParts.length ==  5) {
                String set = cmdParts[0];
                String key = cmdParts[1];
                // nb, technically only 16-32 bits max.  Long allows for parsing as unsigned 32 bits
                Long flags = Long.parseLong(cmdParts[2]);
                
                Long expTime = Long.parseLong(cmdParts[3]);
                
                Long payloadLength = Long.parseLong(cmdParts[4]);
                
                // good, now verify that the payload length is correct.  -2 to account for crlf separating command from payload
                if (payloadLength == commandBytes.length - posn - 2) {
                    byte[] payloadBytes = new byte[payloadLength.intValue()];
                    System.arraycopy(commandBytes, posn + 2, payloadBytes, 0, payloadLength.intValue());
                    
                    // now we have the bytes, set it
                    dataMap.put(key, payloadBytes);
                }
            }
        }
        
    }
    
}
