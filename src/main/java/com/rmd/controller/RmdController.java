
package com.rmd.controller;

import com.rmd.service.RmdService;
import com.rmd.model.RequestModel;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class RmdController {

  private final RmdService service;

  public RmdController(RmdService service) {
    this.service = service;
  }

  @PostMapping("/analyze")
  public Map<String,Object> analyze(@RequestBody RequestModel req) {
    return service.process(req);
  }
}
