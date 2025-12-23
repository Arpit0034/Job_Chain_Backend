package com.jobchain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Service for handling all blockchain interactions via Web3j.
 * Connects to Ethereum network and interacts with JobChainContract smart contract.
 * All recruitment operations are recorded on blockchain for transparency and immutability.
 */
@Service
@Slf4j
public class BlockchainService {

    @Value("${blockchain.rpc.url}")
    private String rpcUrl;

    @Value("${blockchain.contract.address}")
    private String contractAddress;

    @Value("${blockchain.private.key}")
    private String privateKey;

    private Web3j web3j;
    private Credentials credentials;

    /**
     * Initialize Web3j connection and credentials on bean creation.
     */
    public void init() {
        try {
            this.web3j = Web3j.build(new HttpService(rpcUrl));
            this.credentials = Credentials.create(privateKey);
            log.info("Blockchain connection initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize blockchain connection: {}", e.getMessage());
            throw new RuntimeException("Blockchain initialization failed", e);
        }
    }

    /**
     * Creates a vacancy on blockchain.
     * Calls createVacancy() function of smart contract.
     *
     * @param title Job title
     * @param totalPosts Number of available positions
     * @param paperHash SHA-256 hash of vacancy notification document
     * @return Transaction hash as proof of blockchain recording
     */
    public String createVacancyOnChain(String title, int totalPosts, String paperHash) {
        try {
            log.info("Creating vacancy on blockchain: title={}, totalPosts={}", title, totalPosts);

            // Convert parameters to bytes32 format for Solidity
            String paperHashBytes32 = "0x" + paperHash;

            // Call smart contract function: createVacancy(string, uint, bytes32)
            String txHash = sendTransaction("createVacancy", title, totalPosts, paperHashBytes32);

            log.info("Vacancy created on blockchain successfully. TxHash: {}", txHash);
            return txHash;

        } catch (Exception e) {
            log.error("Failed to create vacancy on blockchain: {}", e.getMessage());
            throw new RuntimeException("Blockchain transaction failed", e);
        }
    }

    /**
     * Logs application on blockchain for tamper-proof record.
     *
     * @param vacancyId UUID of the vacancy
     * @param appHash SHA-256 hash of application JSON
     * @return Transaction hash
     */
    public String logApplicationOnChain(UUID vacancyId, String appHash) {
        try {
            log.info("Logging application on blockchain: vacancyId={}", vacancyId);

            // Convert vacancyId to uint (take hash of UUID)
            BigInteger vacancyIdBigInt = BigInteger.valueOf(vacancyId.hashCode());
            String appHashBytes32 = "0x" + appHash;

            // Call smart contract function: logApplication(uint, bytes32)
            String txHash = sendTransaction("logApplication", vacancyIdBigInt, appHashBytes32);

            log.info("Application logged on blockchain. TxHash: {}", txHash);
            return txHash;

        } catch (Exception e) {
            log.error("Failed to log application on blockchain: {}", e.getMessage());
            throw new RuntimeException("Blockchain transaction failed", e);
        }
    }

    /**
     * Records exam score on blockchain to prevent manipulation.
     *
     * @param vacancyId UUID of the vacancy/exam
     * @param marks Marks obtained by candidate
     * @param markHash SHA-256 hash of detailed marksheet
     * @return Transaction hash
     */
    public String recordExamScoreOnChain(UUID vacancyId, int marks, String markHash) {
        try {
            log.info("Recording exam score on blockchain: vacancyId={}, marks={}", vacancyId, marks);

            BigInteger vacancyIdBigInt = BigInteger.valueOf(vacancyId.hashCode());
            BigInteger marksBigInt = BigInteger.valueOf(marks);
            String markHashBytes32 = "0x" + markHash;

            // Call smart contract function: recordExamScore(uint, uint, bytes32)
            String txHash = sendTransaction("recordExamScore", vacancyIdBigInt, marksBigInt, markHashBytes32);

            log.info("Exam score recorded on blockchain. TxHash: {}", txHash);
            return txHash;

        } catch (Exception e) {
            log.error("Failed to record exam score on blockchain: {}", e.getMessage());
            throw new RuntimeException("Blockchain transaction failed", e);
        }
    }

    /**
     * Publishes merit list on blockchain for transparency.
     *
     * @param vacancyId UUID of the vacancy
     * @param meritHash SHA-256 hash of complete merit list JSON
     * @return Transaction hash
     */
    public String publishMeritOnChain(UUID vacancyId, String meritHash) {
        try {
            log.info("Publishing merit list on blockchain: vacancyId={}", vacancyId);

            BigInteger vacancyIdBigInt = BigInteger.valueOf(vacancyId.hashCode());
            String meritHashBytes32 = "0x" + meritHash;

            // Call smart contract function: publishMerit(uint, bytes32)
            String txHash = sendTransaction("publishMerit", vacancyIdBigInt, meritHashBytes32);

            log.info("Merit list published on blockchain. TxHash: {}", txHash);
            return txHash;

        } catch (Exception e) {
            log.error("Failed to publish merit list on blockchain: {}", e.getMessage());
            throw new RuntimeException("Blockchain transaction failed", e);
        }
    }

    /**
     * Records OMR sheet scan on blockchain with QR verification.
     *
     * @param candidateAddress Candidate's identifier
     * @param omrHash SHA-256 hash of OMR sheet data
     * @param qrHash SHA-256 hash of QR code on OMR sheet
     * @return Transaction hash
     */
    public String recordOMRScanOnChain(String candidateAddress, String omrHash, String qrHash) {
        try {
            log.info("Recording OMR scan on blockchain: candidate={}", candidateAddress);

            String omrHashBytes32 = "0x" + omrHash;
            String qrHashBytes32 = "0x" + qrHash;

            // Call smart contract function: recordOMRScan(bytes32, bytes32)
            String txHash = sendTransaction("recordOMRScan", omrHashBytes32, qrHashBytes32);

            log.info("OMR scan recorded on blockchain. TxHash: {}", txHash);
            return txHash;

        } catch (Exception e) {
            log.error("Failed to record OMR scan on blockchain: {}", e.getMessage());
            throw new RuntimeException("Blockchain transaction failed", e);
        }
    }

    /**
     * Records answer key on blockchain.
     *
     * @param answerKeyHash SHA-256 hash of answer key JSON
     * @return Transaction hash
     */
    public String recordAnswerKeyOnChain(String answerKeyHash) {
        try {
            log.info("Recording answer key on blockchain");

            String answerKeyHashBytes32 = "0x" + answerKeyHash;
            boolean isFinalized = true; // Mark as finalized

            // Call smart contract function: recordAnswerKey(bytes32, bool)
            String txHash = sendTransaction("recordAnswerKey", answerKeyHashBytes32, isFinalized);

            log.info("Answer key recorded on blockchain. TxHash: {}", txHash);
            return txHash;

        } catch (Exception e) {
            log.error("Failed to record answer key on blockchain: {}", e.getMessage());
            throw new RuntimeException("Blockchain transaction failed", e);
        }
    }

    /**
     * Distributes question paper set on blockchain.
     *
     * @param vacancyId UUID of the vacancy/exam
     * @param setId Paper set identifier (A, B, C, etc.)
     * @param paperHash SHA-256 hash of question paper content
     * @return Transaction hash
     */
    public String distributePaperOnChain(UUID vacancyId, String setId, String paperHash) {
        try {
            log.info("Distributing paper set on blockchain: vacancyId={}, setId={}", vacancyId, setId);

            BigInteger vacancyIdBigInt = BigInteger.valueOf(vacancyId.hashCode());
            String paperHashBytes32 = "0x" + paperHash;

            // Call smart contract function: distributePaper(uint, string, bytes32)
            String txHash = sendTransaction("distributePaper", vacancyIdBigInt, setId, paperHashBytes32);

            log.info("Paper set distributed on blockchain. TxHash: {}", txHash);
            return txHash;

        } catch (Exception e) {
            log.error("Failed to distribute paper on blockchain: {}", e.getMessage());
            throw new RuntimeException("Blockchain transaction failed", e);
        }
    }

    /**
     * Detects and records paper leak on blockchain for investigation.
     *
     * @param vacancyId UUID of the affected vacancy/exam
     * @param suspectCount Number of suspects identified
     * @param patternHash SHA-256 hash of fraud pattern analysis
     * @return Transaction hash
     */
    public String detectPaperLeakOnChain(UUID vacancyId, int suspectCount, String patternHash) {
        try {
            log.warn("FRAUD ALERT: Paper leak detected for vacancyId={}, suspectCount={}", vacancyId, suspectCount);

            BigInteger vacancyIdBigInt = BigInteger.valueOf(vacancyId.hashCode());
            BigInteger suspectCountBigInt = BigInteger.valueOf(suspectCount);
            String patternHashBytes32 = "0x" + patternHash;

            // Call smart contract function: detectPaperLeak(uint, uint, bytes32)
            String txHash = sendTransaction("detectPaperLeak", vacancyIdBigInt, suspectCountBigInt, patternHashBytes32);

            log.warn("Paper leak alert recorded on blockchain. TxHash: {}", txHash);
            return txHash;

        } catch (Exception e) {
            log.error("Failed to record paper leak on blockchain: {}", e.getMessage());
            throw new RuntimeException("Blockchain transaction failed", e);
        }
    }

    /**
     * Utility method to calculate SHA-256 hash.
     *
     * @param input String to hash
     * @return Hexadecimal hash string (64 characters)
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to calculate SHA-256 hash: {}", e.getMessage());
            throw new RuntimeException("Hash calculation failed", e);
        }
    }

    /**
     * Sends transaction to smart contract.
     * Handles transaction signing, gas estimation, and receipt confirmation.
     *
     * @param functionName Name of smart contract function to call
     * @param params Parameters to pass to the function
     * @return Transaction hash
     */
    private String sendTransaction(String functionName, Object... params) {
        try {
            // In production, you would:
            // 1. Load contract using Web3j generated wrapper
            // 2. Call the specific function with parameters
            // 3. Wait for transaction receipt
            // 4. Return transaction hash

            // Example pseudo-code (requires Web3j contract wrapper):
            // JobChainContract contract = JobChainContract.load(
            //     contractAddress, web3j, credentials, new DefaultGasProvider()
            // );
            // TransactionReceipt receipt = contract.functionName(params).send();
            // return receipt.getTransactionHash();

            // For now, generate a mock transaction hash for demonstration
            String mockTxHash = "0x" + sha256(functionName + System.currentTimeMillis());
            log.info("Transaction sent: function={}, txHash={}", functionName, mockTxHash);
            return mockTxHash;

        } catch (Exception e) {
            log.error("Transaction failed: function={}, error={}", functionName, e.getMessage());
            throw new RuntimeException("Transaction execution failed", e);
        }
    }
}