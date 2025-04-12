package com.cuizhy.rhc.cache;

import com.cuizhy.rhc.constants.Constants;
import com.cuizhy.rhc.model.Info;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CacheUtil {

    private static final Map<String,Object> db0 = new ConcurrentHashMap<>();

    public Object get(String key){
        return db0.get(key);
    }

    public void set(String key, Object value){
        db0.put(key, value);
    }

    public void del(String key){
        db0.remove(key);
    }

    public void flush(){
        db0.clear();
    }

    public Map<String,Object> getDb0(){
        return new HashMap<>(db0);
    }

    public void addInfoToJobList(Info info){
        Map<String,Info> job = (Map<String,Info>) db0.get(Constants.JOB_LIST_KEY);
        if (info !=null){
            job.put(info.getEnv()+"-"+info.getName(), info);
        }
    }

    public Info getInfoFromJobList(String env, String work){
        Map<String,Info> job = (Map<String,Info>) db0.get(Constants.JOB_LIST_KEY);
        return job.get(env+"-"+work);
    }

    static {
        db0.put(Constants.JOB_LIST_KEY, new ConcurrentHashMap<String,Info>());
    }
}
