package com.company.figmaintegrationservice.client;

import com.company.figmaintegrationservice.client.dto.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
//@Primary
public class MockFigmaClient implements IFigmaClient {

    @Override
    public Mono<FigmaFileResponse> getFile(String token, String fileKey) {
        List<PageNode> pages = new ArrayList<>();

        // 20 страниц
        for (int p = 1; p <= 20; p++) {
            List<FigmaNode> topNodes = new ArrayList<>();

            // 10 фреймов на странице
            for (int f = 1; f <= 10; f++) {
                FigmaNode frameNode = createFrameNode(p, f);
                topNodes.add(frameNode);
            }

            // 5 секций внизу страницы
            for (int s = 1; s <= 5; s++) {
                FigmaNode sectionNode = createSectionNode(p, s);
                topNodes.add(sectionNode);
            }

            PageNode page = new PageNode(
                    "page-" + p,
                    "Страница " + p + " - Проект Alpha",
                    "PAGE",
                    topNodes
            );
            pages.add(page);
        }

        DocumentNode document = new DocumentNode(pages);
        return Mono.just(new FigmaFileResponse("v2.3.1", document));
    }

    private FigmaNode createFrameNode(int pageNum, int frameNum) {
        FigmaNode frame = new FigmaNode();
        frame.setId("frame-" + pageNum + "-" + frameNum);
        frame.setName("Основной фрейм " + frameNum);
        frame.setType("FRAME");
        frame.setFrameName("Шапка фрейма " + frameNum);

        List<FigmaNode> children = new ArrayList<>();

        // 5 текстовых блоков
        for (int t = 1; t <= 5; t++) {
            FigmaNode text = new FigmaNode();
            text.setId("text-" + pageNum + "-" + frameNum + "-" + t);
            text.setName("Текст " + t);
            text.setType("TEXT");
            text.setCharacters("Пример текстового содержимого для блока " + t +
                    " на странице " + pageNum + " в фрейме " + frameNum +
                    ". Здесь может быть длинное описание элемента дизайна.");
            text.setFrameName(frame.getName());
            text.setHasImageFill(false);
            children.add(text);
        }

        // 8 изображений
        for (int i = 1; i <= 8; i++) {
            FigmaNode image = new FigmaNode();
            image.setId("image-" + pageNum + "-" + frameNum + "-" + i);
            image.setName("Изображение " + i);
            image.setType("RECTANGLE");
            image.setFrameName(frame.getName());
            image.setHasImageFill(true);
            image.setImageUrl(getRandomImageUrl(pageNum, frameNum, i));
            children.add(image);
        }

        // 3 группы с вложенными элементами
        for (int g = 1; g <= 3; g++) {
            FigmaNode group = new FigmaNode();
            group.setId("group-" + pageNum + "-" + frameNum + "-" + g);
            group.setName("Группа элементов " + g);
            group.setType("GROUP");
            group.setFrameName(frame.getName());

            List<FigmaNode> groupChildren = new ArrayList<>();

            // В каждой группе по 4 элемента
            for (int gc = 1; gc <= 4; gc++) {
                FigmaNode groupText = new FigmaNode();
                groupText.setId("group-text-" + pageNum + "-" + frameNum + "-" + g + "-" + gc);
                groupText.setName("Элемент группы " + gc);
                groupText.setType("TEXT");
                groupText.setCharacters("Вложенный текст группы " + g + " элемент " + gc);
                groupText.setFrameName(frame.getName());
                groupText.setHasImageFill(false);
                groupChildren.add(groupText);
            }

            group.setChildren(groupChildren);
            children.add(group);
        }

        frame.setChildren(children);
        return frame;
    }

    private FigmaNode createSectionNode(int pageNum, int sectionNum) {
        FigmaNode section = new FigmaNode();
        section.setId("section-" + pageNum + "-" + sectionNum);
        section.setName("Секция " + sectionNum);
        section.setType("SECTION");
        section.setFrameName("Нижняя часть страницы");

        List<FigmaNode> children = new ArrayList<>();

        // Заголовок секции
        FigmaNode title = new FigmaNode();
        title.setId("section-title-" + pageNum + "-" + sectionNum);
        title.setName("Заголовок секции");
        title.setType("TEXT");
        title.setCharacters("Секция " + sectionNum + " - важная информация");
        title.setFrameName(section.getName());
        title.setHasImageFill(false);
        children.add(title);

        // 3 карточки в секции
        for (int c = 1; c <= 3; c++) {
            FigmaNode card = new FigmaNode();
            card.setId("card-" + pageNum + "-" + sectionNum + "-" + c);
            card.setName("Карточка " + c);
            card.setType("FRAME");
            card.setFrameName(section.getName());

            List<FigmaNode> cardChildren = new ArrayList<>();

            // Картинка карточки
            FigmaNode cardImage = new FigmaNode();
            cardImage.setId("card-image-" + pageNum + "-" + sectionNum + "-" + c);
            cardImage.setName("Изображение карточки");
            cardImage.setType("RECTANGLE");
            cardImage.setFrameName(card.getName());
            cardImage.setHasImageFill(true);
            cardImage.setImageUrl(getRandomImageUrl(pageNum, sectionNum, c + 100));
            cardChildren.add(cardImage);

            // Текст карточки
            FigmaNode cardText = new FigmaNode();
            cardText.setId("card-text-" + pageNum + "-" + sectionNum + "-" + c);
            cardText.setName("Описание");
            cardText.setType("TEXT");
            cardText.setCharacters("Это описание для карточки " + c +
                    ". Здесь может быть любой текст, описывающий данный элемент.");
            cardText.setFrameName(card.getName());
            cardText.setHasImageFill(false);
            cardChildren.add(cardText);

            card.setChildren(cardChildren);
            children.add(card);
        }

        section.setChildren(children);
        return section;
    }

    private String getRandomImageUrl(int pageNum, int elementNum, int variant) {
        // Используем только один, гарантированно рабочий URL для всех изображений
        return "https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png";
    }

    @Override
    public Mono<FigmaImageResponse> getImages(String token, String fileKey, String nodeIds) {
        FigmaImageResponse response = new FigmaImageResponse();
        response.setImages("https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png");
        return Mono.just(response);
    }

    @Override
    public Mono<FigmaNodesResponse> getNodes(String token, String fileKey, String nodeIds, int depth) {
        // Создаем простой ответ с одним узлом для мока
        Map<String, FigmaNodesResponse.NodeWrapper> nodes = new HashMap<>();
        FigmaNode mockNode = new FigmaNode();
        mockNode.setId("mock-node");
        mockNode.setName("Mock Node");
        mockNode.setType("FRAME");
        
        FigmaNodesResponse.NodeWrapper wrapper = new FigmaNodesResponse.NodeWrapper();
        wrapper.setDocument(mockNode);
        nodes.put("mock-node", wrapper);
        
        FigmaNodesResponse response = new FigmaNodesResponse();
        response.setNodes(nodes);
        return Mono.just(response);
    }
}