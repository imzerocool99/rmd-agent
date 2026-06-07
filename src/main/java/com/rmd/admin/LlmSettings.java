package com.rmd.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mutable LLM settings — loaded from application.properties at startup
 * but can be updated at runtime via the Admin API without restarting.
 */
@Component
public class LlmSettings {

    @Value("${ollama.url:http://localhost:11434/api/generate}")
    private String ollamaUrl;

    @Value("${ollama.model:phi3}")
    private String model;

    @Value("${ollama.temperature:0.3}")
    private double temperature;

    @Value("${ollama.top_p:0.9}")
    private double topP;

    @Value("${ollama.num_predict:256}")
    private int numPredict;

    public String getOllamaUrl()              { return ollamaUrl; }
    public String getModel()                  { return model; }
    public double getTemperature()            { return temperature; }
    public double getTopP()                   { return topP; }
    public int    getNumPredict()             { return numPredict; }

    public void setModel(String model)               { this.model = model; }
    public void setTemperature(double temperature)   { this.temperature = temperature; }
    public void setTopP(double topP)                 { this.topP = topP; }
    public void setNumPredict(int numPredict)         { this.numPredict = numPredict; }
}
