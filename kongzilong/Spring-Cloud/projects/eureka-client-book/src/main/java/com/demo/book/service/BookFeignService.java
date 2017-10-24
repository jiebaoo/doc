package com.demo.book.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * TODO
 *
 * @author zlkong
 * @date 2017年10月24日 下午4:30:44
 *
 */
@FeignClient(value = "BOOK")
public interface BookFeignService {

    @GetMapping("/books")
    String listBooks();

}
