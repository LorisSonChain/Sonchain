package sonchain.blockchain.contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sonchain.blockchain.core.CallTransaction;
import sonchain.blockchain.vm.LogInfo;

public class SonContractCallResultImpl extends SonContractCallResult{
	 private SonContractImpl m_contract = null;
     private CallTransaction.Function m_function = null;

     SonContractCallResultImpl(SonContractImpl contract, CallTransaction.Function function) {
         m_contract = contract;
         m_function = function;
     }

     @Override
     public CallTransaction.Function getFunction() {
         return m_function;
     }

     public List<CallTransaction.Invocation> getEvents() {
         List<CallTransaction.Invocation> ret = new ArrayList<>();
         for (LogInfo logInfo : getReceipt().getLogInfoList()) {
             for (CallTransaction.Contract c : m_contract.m_relatedContracts) {
                 CallTransaction.Invocation event = c.parseEvent(logInfo);
                 if (event != null) ret.add(event);
             }
         }
         return ret;
     }

     @Override
     public String toString() {
         String ret = "SonContractCallResultImpl{" +
        		 m_function + ": " +
                 (isIncluded() ? "EXECUTED" : "PENDING") + ", ";
         if (isIncluded()) {
             ret += isSuccessful() ? "SUCCESS" : ("ERR (" + getReceipt().getError() + ")");
             ret += ", ";
             if (isSuccessful()) {
                 ret += "Ret: " + Arrays.toString(getReturnValues()) + ", ";
                 ret += "Events: " + getEvents() + ", ";
             }
         }
         return ret + "}";
     }
}
