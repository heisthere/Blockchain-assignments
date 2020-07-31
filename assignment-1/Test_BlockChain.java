package assignment3;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Random;

public class Test {
    
    public static void main(String[] args) throws NoSuchAlgorithmException,
    SignatureException, InvalidKeyException
    {
        
        // Create some key pairs to work with
        Random random = new Random();
        int numBitsKeyPair = 512;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(numBitsKeyPair);
        KeyPair scroogeKeyPair = keyPairGenerator.genKeyPair();
        KeyPair aliceKeyPair = keyPairGenerator.genKeyPair();
        KeyPair bobKeyPair = keyPairGenerator.genKeyPair();
        
        // Always create genesis block first
        Block genesis = new Block(null, scroogeKeyPair.getPublic()); // new Block (byte[] prevHash, PublicKey address)
        genesis.finalize(); // Computes the message digest/hash
        
        // Then, create blockchain from genesis block
        BlockChain blockchain = new BlockChain(genesis);
        
        // And initialize BlockHandler with "genesis blockchain".
        // I personally think that the framework is inkonsistent here.
        // If all access to the blockchain is supposed to be done through the BlockHandler,
        // then the blockchain should be initialized there as well.
        // Anyways, further access to the blockchain is done through the BlockHandler
        BlockHandler blockHandler = new BlockHandler(blockchain);
        
        // Now, let the tests begin!
        
        //***********************************
        // Test 1: Process a block with no transactions
        Block block = new Block(genesis.getHash(), aliceKeyPair.getPublic()); // previous: the genesis block
        block.finalize();
        // Process block immediately without adding any transactions
        if (blockHandler.processBlock(block)){ // basically invokes blockchain.addBlock()
            System.out.println("Successfully added valid block. Test 1 is passed.\n");
        }else{
            System.out.println("Failed to add valid block. Test 1 has failed.\n");
        }// again, unit tests are way better than this!
        
        
        //***********************************
        // Test 22: Process a transaction, create a block, process a transaction, create a block, ...
        // For a clean test, recreate the blockchain
        blockchain = new BlockChain(genesis);
        blockHandler = new BlockHandler(blockchain);
        
        boolean testIsPassed = true;
        
        // some transaction to play with
        Transaction transaction;
        Signature signature = Signature.getInstance("SHA256withRSA");
        // start with a successor of the genesis block
        Block previousBlock = genesis;
        
        for (int i = 0; i < 20; i++){
            // create a new transaction in every round
            transaction = new Transaction();
            // every block consists of its hash, a hash of the previous block,
            // exactly one coinbase transaction and a list of other transactions.
            // here, we only want to reassign the coinbase transaction
            transaction.addInput(previousBlock.getCoinbase().getHash(), 0); // addInput(byte[] prevTxHash, int outputIndex)
            
            // let's assign all outputs of all transactions to scrooge
            // I personally would prefer a getter here for the coinbase
            transaction.addOutput(Block.COINBASE, scroogeKeyPair.getPublic()); // transaction.addOutput(double value, PublicKey address)
            signature.initSign(scroogeKeyPair.getPrivate());
            signature.update(transaction.getRawDataToSign(0));
            transaction.addSignature(signature.sign(), 0);
            transaction.finalize(); // Computes the message digest/hash
            
            blockHandler.processTx(transaction); // basically invokes blockchain.addTransaction() that adds transaction to the transaction pool
            
            // so far, we have created a valid transaction that should be available in the transaction pool
            // so let's try to create a block from it
            Block newBlock = blockHandler.createBlock(scroogeKeyPair.getPublic());
            testIsPassed = testIsPassed &&
                newBlock != null &&
                newBlock.getPrevBlockHash().equals(previousBlock.getHash()) &&
                newBlock.getTransactions().size() == 1 &&
                newBlock.getTransaction(0).equals(transaction);
            
            if(!testIsPassed){
                System.out.println(i + "Failed to add block with valid transaction. Test 22 has failed.\n");
                return;
            }
            previousBlock = newBlock;
        }
        System.out.println("Successfully added blocks with valid transactions. Test 22 is passed.\n");
    }
}
