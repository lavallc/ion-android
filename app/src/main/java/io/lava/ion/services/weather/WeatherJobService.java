package io.lava.ion.services.weather;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

import io.lava.ion.receivers.AlarmScheduler;

public class WeatherJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        Intent service = new Intent(getApplicationContext(), WeatherService.class);
        getApplicationContext().startService(service);
        AlarmScheduler.scheduleWeatherJob(getApplicationContext()); // reschedule the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}