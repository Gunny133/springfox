/*
 *
 *  Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package springfox.documentation.spring.web.scanners;

import com.fasterxml.classmate.TypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ApiListingBuilderPlugin;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.ApiListingContext;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Sets.*;
import static org.springframework.core.annotation.AnnotationUtils.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MediaTypeReader implements OperationBuilderPlugin, ApiListingBuilderPlugin {

  private final TypeResolver typeResolver;

  @Autowired
  public MediaTypeReader(TypeResolver typeResolver) {
    this.typeResolver = typeResolver;
  }

  @Override
  public void apply(OperationContext context) {

    Set<String> consumesList = toSet(context.consumes());
    Set<String> producesList = toSet(context.produces());

    if (handlerMethodHasFileParameter(context)) {
      consumesList = newHashSet(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    if (producesList.isEmpty()) {
      producesList.add(MediaType.ALL_VALUE);
    }
    if (consumesList.isEmpty()) {
      consumesList.add(MediaType.APPLICATION_JSON_VALUE);
    }
    context.operationBuilder().consumes(consumesList);
    context.operationBuilder().produces(producesList);
  }

  @Override
  public void apply(ApiListingContext context) {
    RequestMapping annotation = findAnnotation(context.getResourceGroup().getControllerClass(), RequestMapping.class);
    if (annotation != null) {
      context.apiListingBuilder()
              .appendProduces(newArrayList(annotation.produces()))
              .appendConsumes(newArrayList(annotation.consumes()));
    }
  }

  @Override
  public boolean supports(DocumentationType delimiter) {
    return true;
  }

  private boolean handlerMethodHasFileParameter(OperationContext context) {

    HandlerMethodResolver handlerMethodResolver = new HandlerMethodResolver(typeResolver);
    List<ResolvedMethodParameter> methodParameters = handlerMethodResolver.methodParameters(context.getHandlerMethod());

    for (ResolvedMethodParameter resolvedMethodParameter : methodParameters) {
      if (MultipartFile.class.isAssignableFrom(resolvedMethodParameter.getResolvedParameterType().getErasedType())) {
        return true;
      }
    }
    return false;
  }

  private Set<String> toSet(Set<MediaType> mediaTypeSet) {
    Set<String> mediaTypes = newHashSet();
    for (MediaType mediaType : mediaTypeSet) {
      mediaTypes.add(mediaType.toString());
    }
    return mediaTypes;
  }
}
