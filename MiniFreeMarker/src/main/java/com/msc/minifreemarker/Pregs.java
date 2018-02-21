/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.msc.minifreemarker;

/**
 *
 * @author micky
 */
class Pregs {

    public String originalKey;
    public String key;
    public String values;

    /**
     * Si c'est un point on met l'autre partie dans value.
     */
    public void split() {
        if (originalKey.contains(".")) {
            String[] as = originalKey.split("\\.");
            key = as[0];
            values = as[1];
        } else {
            key = originalKey;
        }
    }

}
