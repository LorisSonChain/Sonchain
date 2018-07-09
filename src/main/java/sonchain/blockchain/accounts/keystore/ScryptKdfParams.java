package sonchain.blockchain.accounts.keystore;

/**
 * ScryptKdf
 *
 */
public class ScryptKdfParams extends KdfParams{

     @Override
     public boolean equals(Object o) {
         if (this == o) {
             return true;
         }
         if (!(o instanceof ScryptKdfParams)) {
             return false;
         }
         
         ScryptKdfParams that = (ScryptKdfParams) o;
         
         if (m_dklen != that.m_dklen) {
             return false;
         }
         if (m_n != that.m_n) {
             return false;
         }
         if (m_p != that.m_p) {
             return false;
         }
         if (m_r != that.m_r) {
             return false;
         }
         return m_salt != null
             ? m_salt.equals(that.m_salt) : that.m_salt == null;
     }  

     @Override
     public int hashCode() {
         int result = m_dklen;
         result = 31 * result + m_n;
         result = 31 * result + m_p;
         result = 31 * result + m_r;
         result = 31 * result + (m_salt != null ? m_salt.hashCode() : 0);
         return result;
     }     
}
