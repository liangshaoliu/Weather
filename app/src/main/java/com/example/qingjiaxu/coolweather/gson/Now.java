package com.example.qingjiaxu.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class Now {

//    "now":
//    {"cloud":"0",
//            "cond_code":"100",
//            "cond_txt":"晴",
//            "fl":"-2",
//            "hum":"16",
//            "pcpn":"0.0",
//            "pres":"1036",
//            "tmp":"3",
//            "vis":"35",
//            "wind_deg":"307",
//            "wind_dir":"西北风",
//            "wind_sc":"3",
//            "wind_spd":"17",
//            "cond":{"code":"100","txt":"晴"}}

    @SerializedName("tmp")
    public String temperature;

    @SerializedName("wind_dir")
    public String windDirection;

    @SerializedName("wind_sc")
    public String windForce;

    @SerializedName("cond")
    public More more;

    public class More {

        @SerializedName("txt")
        public String info;

    }

    //public class
}
