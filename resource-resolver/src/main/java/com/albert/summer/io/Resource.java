package com.albert.summer.io;

/**
 * @author yangjunwei
 * @date 2024/7/16
 */
public record Resource(String path,String name) {

    public static void main(String[] args) {
        Resource summer = new Resource("/", "summer");
        if(summer.equals(new Resource("/", "summer"))) {
            System.out.println("equals");
        }
    }


}
