/**
 * 全局变量及方法
 */

export default {
    loginState:'hub',
    apiUrl:'',
    epochBeginning: -1,
    newConsole:null,
    isOpenConsole:false,
    blockchainState:[],
    peers:[],
    userConfig:[],

    fetch(type, date, requestType) {
        return new Promise(function (resolve, reject) {
            $.ajax({
                url: window.api.sharderUrl + "?requestType=" + requestType,
                dataType: "json",
                type: type,
                data: date,
                success: function (data) {
                    resolve(data)
                },
                error: function (error) {
                    reject(error)
                }
            })
        })
    },
    /**
     * 获取区块链信息
     * @param t
     * @returns {Promise<any>}
     */
    setBlockchainState(t) {
        const _this = this;
        return new Promise(function (resolve, reject) {
                _this.blockchainState =
                    t.$http.get('/sharder?requestType=getBlockchainStatus',{
                        params:{
                            random:parseInt(new Date().getTime().toString())
                        }
                    }).then(res => {res.data;
                resolve(res);
            });
        });
    },
    /**
     * 获取未出快的交易
     * @param t
     * @param account
     * @returns {Promise<any>}
     */
    setUnconfirmedTransactions(t,account){
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get('/sharder?requestType=getUnconfirmedTransactions',{
                params:{
                    random:parseInt(new Date().getTime().toString()),
                    account:account
                }
            }).then(res => {
                resolve(res);
            });
        });
    },
    /**
     * 获取节点
     * @param t
     * @returns {Promise<any>}
     */
    setPeers(t){
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get('/sharder?requestType=getPeers',{
                params: {
                    includePeerInfo:true,
                    random:parseInt(new Date().getTime().toString())
                }
            }).then(res => {
                _this.peers = res.data;
                resolve(res);
                // console.log(res.data);
                // if (_this.isOpenConsole) {
                //     console.log(res.data);
                // }
            });
        });
    },
    /**
     * 获取创世时间
     * @param t
     * @returns {Promise<any>}
     */
    setEpochBeginning(t) {
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get("/sharder?requestType=getConstants").then(function (res) {
                _this.epochBeginning = parseInt(res.data.epochBeginning);
                resolve(res.data.epochBeginning);
            });
        });
    },
    /**
     * 获取在创世时间后的时间
     * @param value
     * @param type
     * @returns {string}
     */
    myFormatTime(value, type,hasEpochBeginning) {
        const _this = this;
        let dataTime = "";
        let data = new Date();
        if(typeof value === 'undefined')
            value = "0";

        let date = parseInt(value + '000');
        if(hasEpochBeginning){
            date = date + _this.epochBeginning;
        }
        data.setTime(date);
        let year = data.getFullYear();
        let month = _this.addZero(data.getMonth() + 1);
        let day = _this.addZero(data.getDate());
        let hour = _this.addZero(data.getHours());
        let minute = _this.addZero(data.getMinutes());
        let second = _this.addZero(data.getSeconds());
        if (type === "YMD") {
            dataTime = year + "-" + month + "-" + day;
        } else if (type === "YMDHMS") {
            dataTime = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
        } else if (type === "HMS") {
            dataTime = hour + ":" + minute + ":" + second;
        } else if (type === "YM") {
            dataTime = year + "-" + month;

        }
        return dataTime;//将格式化后的字符串输出到前端显示
    },
    /**
     *格式化时间 加0
     * @param val
     * @returns {*}
     */
    addZero(val) {
        if (val < 10) {
            return "0" + val;
        } else {
            return val;
        }
    },
    /**
     * 格式化金额
     * @param s
     * @returns {string}
     */
    formatMoney(s,n=8) {
/*        let result = '', counter = 0;
        num = (num || 0).toString();
        for (let i = num.length - 1; i >= 0; i--) {
            counter++;
            result = num.charAt(i) + result;
            if (!(counter % 3) && i !== 0) {
                result = ',' + result;
            }
        }
        return result;*/
        if(isNaN(s)){
            return 0;
        }

        n = n >= 0 && n <= 20 ? n : 2;
        s = parseFloat((s + "").replace(/[^\d\.-]/g, "")).toFixed(n) + "";
        let l = s.split(".")[0].split("").reverse(),
            r = s.split(".")[1].replace(/(0+)\b/,'');         //去除末尾的零
        let t = "";
        for (let i = 0; i < l.length; i++) {
            t += l[i] + ((i + 1) % 3 === 0 && (i + 1) !== l.length ? "," : "");
        }
        /*let num = t.split("").reverse().join("") + "." + r;

        num = parseFloat(num).toString();
        return num;*/
        if(r === ''){
            return t.split("").reverse().join("")
        }else
            return t.split("").reverse().join("") + "." + r;


    },
    /**
     * IP 数组 查询 坐标
     */
    byIPtoCoordinates(params) {
        let xhr = new XMLHttpRequest();
        xhr.open("POST", "https://sharder.org/api/front/coordinates/ip");
        xhr.setRequestHeader("content-type", "application/json;charset=UTF-8");
        return new Promise(function (resolve, reject) {
            xhr.onload = function () {
                resolve(xhr.response);
            };
            xhr.onerror = function (error) {
                reject(error);
            };
            xhr.send(JSON.stringify(params));
        });
    },
    /**
     * 获取用户配置
     * @param t
     * @returns {Promise<any>}
     */
    getUserConfig(t){
        const _this = this;
        return new Promise(function (resolve, reject) {
            t.$http.get('/sharder?requestType=getUserConfig',{
                params:{
                    random:new Date().getTime().toString()
                }
            }).then(res=>{
                _this.userConfig = res.data;
                resolve(res.data);
            }).catch(err=>{
                console.log(err);
            });
        });
    },
    /**
     * string转为byte[]
     * @param str
     * @returns {Array}
     */
    stringToByte(str) {
        let bytes = [];
        let len, c;
        len = str.length;
        for(let i = 0; i < len; i++) {
            c = str.charCodeAt(i);
            if(c >= 0x010000 && c <= 0x10FFFF) {
                bytes.push(((c >> 18) & 0x07) | 0xF0);
                bytes.push(((c >> 12) & 0x3F) | 0x80);
                bytes.push(((c >> 6) & 0x3F) | 0x80);
                bytes.push((c & 0x3F) | 0x80);
            } else if(c >= 0x000800 && c <= 0x00FFFF) {
                bytes.push(((c >> 12) & 0x0F) | 0xE0);
                bytes.push(((c >> 6) & 0x3F) | 0x80);
                bytes.push((c & 0x3F) | 0x80);
            } else if(c >= 0x000080 && c <= 0x0007FF) {
                bytes.push(((c >> 6) & 0x1F) | 0xC0);
                bytes.push((c & 0x3F) | 0x80);
            } else {
                bytes.push(c & 0xFF);
            }
        }
        return bytes;
    },
    /**
     * byte[]转为string
     * @param arr
     * @returns {string}
     */
    byteToString(arr) {
        if(typeof arr === 'string') {
            return arr;
        }
        let str = '',
            _arr = arr;
        for(let i = 0; i < _arr.length; i++) {
            let one = _arr[i].toString(2),
                v = one.match(/^1+?(?=0)$/);
            if(v && one.length === 8) {
                let bytesLength = v[0].length;
                let store = _arr[i].toString(2).slice(7 - bytesLength);
                for(let st = 1; st < bytesLength; st++) {
                    store += _arr[st + i].toString(2).slice(2);
                }
                str += String.fromCharCode(parseInt(store, 2));
                i += bytesLength - 1;
            } else {
                str += String.fromCharCode(_arr[i]);
            }
        }
        return str;
    },

    /**
     * 将数据添加进打开的命令行窗口
     * @param url
     * @param type
     * @param data
     * @param response
     * @param error
     */
    addToConsole(url, type, data, response, error) {
        const _this = this;
        if(!_this.isOpenConsole || !_this.newConsole){
            return;
        }
        if(!_this.newConsole.document || !_this.newConsole.document.body){
            _this.isOpenConsole = false;
            _this.newConsole = null;
            return;
        }
        url = url.replace(/&random=[\.\d]+/, "",url);
        _this.addToConsoleBody(url+ "("+type+")"+new Date().toString(), "url");

        if (data) {
            if (typeof data === "string") {
                let d = _this.queryStringToObject(data);
                _this.addToConsoleBody(JSON.stringify(d, null, "\t"), "post");
            } else {
                _this.addToConsoleBody(JSON.stringify(data, null, "\t"), "post");
            }
        }

        if (error) {
            _this.addToConsoleBody(response, "error");
        } else {
            if(typeof response === 'undefined'){
                return;
            }else{
                _this.addToConsoleBody(JSON.stringify(response, null, "\t"), (response.errorCode ? "error" : ""));
            }
        }
    },
    addToConsoleBody(text,type){
        const _this = this;
        let color = "";
        switch (type) {
            case "url":
                color = "#29FD2F";
                break;
            case "post":
                color = "lightgray";
                break;
            case "error":
                color = "red";
                break;
        }
        _this.logConsole(text, true);
        $(_this.newConsole.document.body).find("#console").append("<pre" + (color ? " style='color:" + color + "'" : "") + ">" + text.escapeHTML() + "</pre>");

    },
    logConsole(msg, isDateIncluded, isDisplayTimeExact){

    },
    queryStringToObject(qs){
        qs = qs.split("&");

        if (!qs) {
            return {};
        }

        let obj = {};

        for (let i = 0; i < qs.length; ++i) {
            let p = qs[i].split('=');

            if (p.length !== 2) {
                continue;
            }

            obj[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
        }
        if ("secretPhrase" in obj) {
            obj.secretPhrase = "***";
        }

        return obj;
    },

    setJsonLocalStorage:function (key,val) {
        try {
            localStorage.setItem(key,JSON.stringify(val));
        }catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }

    },
    getJsonLocalStorage:function (key) {
        try {
            return JSON.parse(localStorage.getItem(key));
        }catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }
    },
    setlocalStorage:function(key,val){
        try {
            localStorage.setItem(key,val);
        }catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }
    },
    getLocalStorage:function (key) {
        try {
            return localStorage.getItem(key);
        }catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }
    },
    setlang:function(_lang){
        try {
            localStorage.lang = _lang;
        } catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }
    },
    lang:function(){
        try {
            return localStorage.lang;
        } catch (e) {
            console.warn("The browser does not support cookies.");
            return null;
        }
    },

    getAvgTimestamp:function(lastBlockTimestamp, firstBlockTimestamp, length){
        let avgTimestamp = parseInt((lastBlockTimestamp - firstBlockTimestamp)/length);
/*
        let minute = parseInt(avgTimestamp/60);
        let hour = parseInt(minute/60);
        let day = parseInt(hour/24);
        let second = avgTimestamp - minute * 60;

        console.log("lastBlockTimestamp",lastBlockTimestamp);
        console.log("firstBlockTimestamp",firstBlockTimestamp);
        console.log("length",length);
        console.log("avgTimestamp",avgTimestamp);
        console.log("second",second);
        console.log("minute",minute);
        console.log("hour",hour);
        console.log("day",day);

        let dataTime = "";
        if (day !== 0) {
            dataTime = dataTime + day + " (day) ";
        }
        if (hour !== 0) {
            dataTime = dataTime + hour + " (hour) ";
        }
        if (minute !== 0) {
            dataTime = dataTime + minute + " (min) ";
        }
        if (second !== 0) {
            dataTime = dataTime + second + " (s) ";
        }

        return dataTime;//将格式化后的字符串输出到前端显示
*/
        return avgTimestamp;
    },

    //将科学计数法转换为小数
    toNonExponential:function(num){
        let m = num.toExponential().match(/\d(?:\.(\d*))?e([+-]\d+)/);
        console.log("m",m);
        let result = num.toFixed(Math.max(0, (m[1] || '').length - m[2]));
        console.log("result",result);
        return result;
    }
};