package com.programmers.vouchermanagement.repository.voucher;

import com.programmers.vouchermanagement.common.ErrorMessage;
import com.programmers.vouchermanagement.domain.voucher.Voucher;
import com.programmers.vouchermanagement.domain.voucher.VoucherFactory;
import com.programmers.vouchermanagement.domain.voucher.VoucherType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("dev")
public class FileVoucherRepository implements VoucherRepository {
    private static final String CSV_SEPARATOR = ",";

    private final String csvFilePath;
    private final Map<UUID, Voucher> vouchers = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(FileVoucherRepository.class);

    public FileVoucherRepository(@Value("${csv.file.voucher.path}") String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    @PostConstruct
    public void init() {
        readFile();
    }

    @Override
    public List<Voucher> findAll() {
        return vouchers.values().stream().toList();
    }

    @Override
    public Voucher save(Voucher voucher) {
        vouchers.put(voucher.getId(), voucher);
        updateFile();
        return voucher;
    }

    public void readFile() {
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            while ((line = br.readLine()) != null) {
                String[] strings = line.split(CSV_SEPARATOR);
                Voucher voucher = VoucherFactory.createVoucher(
                        UUID.fromString(strings[0]),
                        strings[1],
                        Float.parseFloat(strings[2]),
                        VoucherType.valueOf(strings[3].toUpperCase()));
                vouchers.put(voucher.getId(), voucher);
            }
        } catch (FileNotFoundException e) {
            logger.warn(MessageFormat.format("{0} : {1}", ErrorMessage.FILE_NOT_FOUND_MESSAGE.getMessage(), csvFilePath));
        } catch (IOException e) {
            logger.error("Error occurred at FileReader: ", e);
        }
    }

    public void updateFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFilePath))) {
            vouchers.values().stream()
                    .map(voucher -> voucher.joinInfo(CSV_SEPARATOR))
                    .forEach(line -> {
                        try {
                            bw.write(line);
                            bw.newLine();
                        } catch (IOException e) {
                            logger.error("Error occurred at FileWriter: ", e);
                        }
                    });
        } catch (IOException e) {
            logger.error("Error occurred af FileWriter: ", e);
        }
    }
}
