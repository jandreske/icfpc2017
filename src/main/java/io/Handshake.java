package io;

public class Handshake {

    public static class Request {

        private final String me;

        public Request(String me) {
            this.me = me;
        }

        public String getMe() {
            return me;
        }
    }

    public static class Response {

        private String you;

        public Response(String you) {
            this.you = you;
        }

        public Response() {}

        public String getYou() {
            return you;
        }
    }

}
