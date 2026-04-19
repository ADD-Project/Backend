package com.example.ADD.project.backend.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        // 404 등의 에러 발생 시 React의 진입점인 index.html로 포워딩하여 React Router가 처리하도록 함
        return "forward:/index.html";
    }
}