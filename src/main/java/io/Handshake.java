package io;

public class Handshake {

    public static class Request {

        private final String me;

        public Request(String myName) {
            me = myName;
        }

        public String getMe() {
            return me;
        }
    }

    public static class Response {

        private final String you;

        public Response(String name) {
            you = name;
        }

        public String getYou() {
            return you;
        }
    }

}
