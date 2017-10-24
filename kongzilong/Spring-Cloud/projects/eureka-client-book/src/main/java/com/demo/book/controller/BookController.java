package com.demo.book.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.book.service.BookFeignService;

/**
 * TODO
 *
 * @author zlkong
 * @date 2017年10月24日 下午4:06:23
 *
 */

@RestController
@RequestMapping("/books")
public class BookController {

    @Autowired
    BookFeignService bookFeignService;

    @GetMapping
    public String list() {
        return "Hello books!!";
    }

    @GetMapping("/feign")
    public String feign() {
        return bookFeignService.listBooks();
    }

}
