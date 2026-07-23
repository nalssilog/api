package com.nalssilog.member.application;

import com.nalssilog.member.domain.Member;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * {@code 수식어 + 명사 + 세 자리 숫자} 형식의 기본 닉네임을 만든다.
 * 단어 목록은 nickname-modifiers.txt, nickname-nouns.txt 에서 관리한다.
 */
@Component
public class NicknameGenerator {

    private static final int NUMBER_ORIGIN = 100;
    private static final int NUMBER_BOUND = 1_000;

    private final List<String> modifiers;
    private final List<String> nouns;

    public NicknameGenerator() {
        this.modifiers = loadWords("nickname-modifiers.txt");
        this.nouns = loadWords("nickname-nouns.txt");
    }

    public String generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String nickname = modifiers.get(random.nextInt(modifiers.size()))
                + nouns.get(random.nextInt(nouns.size()))
                + random.nextInt(NUMBER_ORIGIN, NUMBER_BOUND);

        if (nickname.length() > Member.NICKNAME_STORAGE_MAX_LENGTH || !nickname.matches(Member.NICKNAME_PATTERN)) {
            throw new IllegalStateException(
                    "자동 생성 닉네임은 공백·특수문자 없이 "
                            + Member.NICKNAME_STORAGE_MAX_LENGTH + "자 이하여야 합니다: " + nickname);
        }

        return nickname;
    }

    private static List<String> loadWords(String resourceName) {
        ClassPathResource resource = new ClassPathResource(resourceName);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> words = reader.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.replaceAll("\\s+", ""))
                    .filter(line -> !line.isEmpty())
                    .toList();

            if (words.isEmpty()) {
                throw new IllegalStateException(resourceName + "에 닉네임 단어를 하나 이상 등록해 주세요.");
            }

            return words;
        } catch (IOException e) {
            throw new IllegalStateException("닉네임 단어 목록을 읽지 못했습니다: " + resourceName, e);
        }
    }
}
