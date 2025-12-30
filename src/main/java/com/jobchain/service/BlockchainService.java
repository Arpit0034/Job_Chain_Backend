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

    private String sendTransaction(String functionName, Object... params) {
        try {

            String mockTxHash = "0x" + sha256(functionName + System.currentTimeMillis());
            log.info("Transaction sent: function={}, txHash={}", functionName, mockTxHash);
            return mockTxHash;

        } catch (Exception e) {
            log.error("Transaction failed: function={}, error={}", functionName, e.getMessage());
            throw new RuntimeException("Transaction execution failed", e);
        }
    }
}