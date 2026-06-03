package com.rmd.monitor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class MonitorService {

    private final List<String> logs = new ArrayList<>();

    public void log(String msg){
        String log = new Date().toString() + " : " + msg;
        logs.add(log);
        System.out.println(log);
    }

    public void alert(String msg){
        String log = "ALERT: " + msg;
        logs.add(log);
        System.err.println(log);
    }

    public List<String> getLogs(){
        return logs;
    }
}