package io;

public class Setup {

    public static class Request {

        private int punter;

        private int punters;

        private Map map;

        public int getPunter() {
            return punter;
        }

        public int getPunters() {
            return punters;
        }

        public Map getMap() {
            return map;
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
    }

    public static class Response {

        private int ready;

        public Response(int myId) {
            ready = myId;
        }

        public int getReady() {
            return ready;
        }
    }

}
