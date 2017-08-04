package io;

public class Setup {

    public static class Request {

        private final int punter;

        private final int punters;

        private final Map map;

        public Request(int punter, int punters, Map map) {
            this.punter = punter;
            this.punters = punters;
            this.map = map;
        }

        public int getPunter() {
            return punter;
        }

        public int getPunters() {
            return punters;
        }

        public Map getMap() {
            return map;
        }
    }

    public static class Response {

        private final int ready;

        public Response(int myId) {
            ready = myId;
        }

        public int getReady() {
            return ready;
        }
    }

}
