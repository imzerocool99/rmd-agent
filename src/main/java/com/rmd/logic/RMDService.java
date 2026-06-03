package com.rmd.logic;

import org.springframework.stereotype.Service;

@Service
public class RMDService {

    public double calculateRMD(int age, double balance){

        // ✅ Simplified IRS factor (example)
        double factor = 27.4;

        if(age >= 75){
            factor = 24.6;
        }

        return balance / factor;
    }
}