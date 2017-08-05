package io;

import com.fasterxml.jackson.annotation.JsonInclude;
import state.GameState;

public class Setup {

    public static class Request {

        private int punter;

        private int punters;

        private Map map;

        private Settings settings;

        public int getPunter() {
            return punter;
        }

        public int getPunters() {
            return punters;
        }

        public Map getMap() {
            return map;
        }

        public Settings getSettings() {
            return settings;
        }

        public void setPunters(int punters) {
            this.punters = punters;
        }

        public void setPunter(int punter) {
            this.punter = punter;
        }

        public void setMap(Map map) {
            this.map = map;
        }

        public void setSettings(Settings settings) {
            this.settings = settings;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {

        private int ready;

        private GameState state = null;

        private Future[] futures;

        public Response(int myId) {
            ready = myId;
        }

        public int getReady() {
            return ready;
        }

        public GameState getState() {
            return state;
        }

        public Future[] getFutures() {
            return futures;
        }

        public void setState(GameState state) {
            this.state = state;
        }

        public void setFutures(Future[] futures) {
            this.futures = futures;
        }
    }

}
