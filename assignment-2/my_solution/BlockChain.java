// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.Iterator;


public class BlockChain {

    

    private ArrayList< BlockNode> blockChain;
    private BlockNode highestNode;
    private TransactionPool txPool;

    public static final int CUT_OFF_AGE = 10;


    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
    	// initialize blockchain as a list
        blockChain = new ArrayList<>();
        UTXOPool utxoPool = new UTXOPool();
        addCoinbaseToUTXOPool(genesisBlock, utxoPool);
        // attribute to find the node within the blockchain
        ByteArrayWrapper wrapper = new ByteArrayWrapper(genesisBlock.getHash());        
        BlockNode genesisNode = new BlockNode(genesisBlock,wrapper,null,utxoPool);        
        blockChain.add(genesisNode);
        txPool = new TransactionPool();
        highestNode = genesisNode;
    }

    /**
     * Get the maximum height block
     */
    public Block getMaxHeightBlock() {
        return highestNode.block;
    }

    /**
     * Get the UTXOPool for mining a new block on top of max height block
     */
    public UTXOPool getMaxHeightUTXOPool() {
        return highestNode.getUTXOPoolCopy();
    }

    /**
     * Get the transaction pool to mine a new block
     */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * <p>
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        byte[] prevBlockHash = block.getPrevBlockHash();
        if (prevBlockHash == null)
            return false;  
        // identify parent node
        ByteArrayWrapper parentWrapper = new ByteArrayWrapper(prevBlockHash); 
        // link node to the parent Node
        BlockNode parentBlockNode = findParentNode(parentWrapper);
        
        if (parentBlockNode == null) {
            return false;
        }
        // initialize new TxHandler to mine on top of parent node
        TxHandler handler = new TxHandler(parentBlockNode.getUTXOPoolCopy());
        Transaction[] txs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = handler.handleTxs(txs);
        if (validTxs.length != txs.length) {
            return false;
        }
        int updatedHeight = parentBlockNode.height + 1;
        if (updatedHeight <= highestNode.height - CUT_OFF_AGE) {
            return false;
        }
        UTXOPool utxoPool = handler.getUTXOPool();
        addCoinbaseToUTXOPool(block, utxoPool);
        ByteArrayWrapper wrapper = new ByteArrayWrapper(block.getHash());
        BlockNode node = new BlockNode(block, wrapper,parentBlockNode, utxoPool);
        
        blockChain.add(node);
        if (updatedHeight > highestNode.height) {
            highestNode = node;
        }
        return true;
    }

    /**
     * Add a transaction to the transaction pool
     */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }

    private void addCoinbaseToUTXOPool(Block block, UTXOPool utxoPool) {
        Transaction coinbase = block.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            utxoPool.addUTXO(utxo, out);
        }
    }
    public class BlockNode {
        public Block block;
        public ByteArrayWrapper wrapper;
        public BlockNode parentNode;
        public ArrayList<BlockNode> children;
        public int height;       
        private UTXOPool uPool;

        public BlockNode(Block block, ByteArrayWrapper wrapper,BlockNode parent, UTXOPool uPool) {
            this.block = block;
            this.parentNode = parent;
            this.wrapper = wrapper;
            children = new ArrayList<>();
            this.uPool = uPool;
            if (parent != null) {
                height = parent.height + 1;
                parent.children.add(this);
            } else {
                height = 1;
            }
        }

        public UTXOPool getUTXOPoolCopy() {
            return new UTXOPool(uPool);
        }

		public ByteArrayWrapper getParentWrapper() {
			return wrapper;
		}
    }
    //method to find a node in the blockchain with the wrapper of its hash
    public BlockNode findParentNode(
    		ByteArrayWrapper parentWrapper) {
    		    Iterator<BlockNode> iterator = blockChain.iterator();
    		    while (iterator.hasNext()) {
    		        BlockNode parentNode = iterator.next();
    		        if (parentNode.getParentWrapper().equals(parentWrapper)) {
    		            return parentNode;
    		        }
    		    }
    		    return null;
    		}
    
}
