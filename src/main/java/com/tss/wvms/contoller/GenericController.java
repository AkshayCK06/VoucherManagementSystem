package com.tss.wvms.contoller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tss.wvms.service.GenericFunctions;


@RestController
@RequestMapping("/api/messages")
public class GenericController {

    private final GenericFunctions genericFunctions;

    public GenericController(GenericFunctions genericFunctions) {
        this.genericFunctions = genericFunctions;
    }

    
    @GetMapping("/send/{self}/{msisdn}/{transactionID}/{msgcontent}")
    public ResponseEntity<String> sendMessageC(
            @PathVariable String self, 
            @PathVariable String msisdn, 
            @PathVariable String transactionID, 
            @PathVariable String msgcontent) {

        if (self.isEmpty() || msisdn.isEmpty() || transactionID.isEmpty() || msgcontent.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid input parameters.");
        }

        System.out.println("this is sent from the postman- self:: "+self+" msisdn:: "+msisdn+" tID:: "+transactionID+" msg:: "+msgcontent);
        genericFunctions.sendMessage(self, msisdn, transactionID, msgcontent);

        return ResponseEntity.ok("Message sent successfully.");
    }
}

