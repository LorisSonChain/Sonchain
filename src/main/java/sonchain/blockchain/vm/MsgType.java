package sonchain.blockchain.vm;

public enum MsgType {
      CALL,
      CALLCODE,
      DELEGATECALL,
      POST;

      /**
       *  Indicates that the code is executed in the context of the caller
       */
      public boolean isStateless() {
          return this == CALLCODE || this == DELEGATECALL;
      }

      public static MsgType fromOpcode(OpCode opCode) {
          switch (opCode) {
              case CALL: 
            	  return CALL;
              case CALLCODE: 
            	  return CALLCODE;
              case DELEGATECALL: 
            	  return DELEGATECALL;
              default:
                  throw new RuntimeException("Invalid call opCode: " + opCode);
          }
      }
  }
