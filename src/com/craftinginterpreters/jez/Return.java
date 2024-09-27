package com.craftinginterpreters.jez;
    //wrap return value with requirements for runtime exception
    class Return extends RuntimeException {
        final Object value;

        Return(Object value) {
            super(null, null, false, false);
            this.value = value;
        }
    }

