package com.example.gstrecon.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(Model model){model.addAttribute("error","Upload limit exceeded. Each workbook must be 20 MB or smaller.");return "upload";}
}
