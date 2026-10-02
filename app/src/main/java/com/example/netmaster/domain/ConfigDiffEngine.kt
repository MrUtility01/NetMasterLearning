package com.example.netmaster.domain

import com.example.netmaster.data.ConfigDiffLine

class ConfigDiffEngine {
    fun diff(old:String,new:String):List<ConfigDiffLine>{
        val a=normalizeLines(old);val b=normalizeLines(new);val out=mutableListOf<ConfigDiffLine>();var i=0;var j=0
        while(i<a.size||j<b.size){
            val x=a.getOrNull(i);val y=b.getOrNull(j)
            when{
                x==y->{out+=ConfigDiffLine(" ",x.orEmpty(),maxOf(i,j)+1);i++;j++}
                x!=null&&y!=null&&a.drop(i+1).take(3).contains(y)->{out+=ConfigDiffLine("-",x,i+1);i++}
                x!=null&&y!=null&&b.drop(j+1).take(3).contains(x)->{out+=ConfigDiffLine("+",y,j+1);j++}
                x!=null->{out+=ConfigDiffLine("-",x,i+1);i++}
                y!=null->{out+=ConfigDiffLine("+",y,j+1);j++}
            }
        }
        return out
    }
    fun fingerprint(text:String)=normalizeLines(text).filter{it.isNotBlank()}.joinToString("\n").hashCode().toString(16)
    private fun normalizeLines(text:String)=text.replace("\r\n","\n").replace('\r','\n').lines()
}
