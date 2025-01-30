package com.tss.wvms.contoller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.tss.wvms.service.WVMSICPInterface;

import java.util.Map;

@RestController
@RequestMapping("/wvms")
public class WVMScontroller {

    @Autowired
    private WVMSICPInterface wvmsICPInterface;

    @PostMapping("/recharge")
    public String recharge(
            @RequestParam String subNos,
            @RequestParam String transId,
            @RequestParam String faceVal,
            @RequestParam String serialNum,
            @RequestParam String batchNum,
            @RequestParam String comment,
            @RequestParam String channel,
            @RequestParam String vAmount,
            @RequestParam String voucherNumber,
            @RequestBody Map<String, String> hash) {

        return wvmsICPInterface.recharge(
                subNos, transId, faceVal, serialNum, batchNum, comment,
                channel, vAmount, voucherNumber, hash
        );
    }

    @GetMapping("/query")
    public String queryTransaction(@RequestParam String transId) {
        return wvmsICPInterface.queryTransaction(transId);
    }
}

