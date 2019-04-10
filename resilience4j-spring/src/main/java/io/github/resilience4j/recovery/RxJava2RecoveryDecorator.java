/*
 * Copyright 2019 Kyuhyen Hwang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.recovery;

import io.reactivex.*;
import io.vavr.CheckedFunction0;

import java.util.Set;
import java.util.function.Function;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

/**
 * recovery decorator for {@link ObservableSource}, {@link SingleSource}, {@link CompletableSource}, {@link MaybeSource} and {@link Flowable}.
 */
public class RxJava2RecoveryDecorator implements RecoveryDecorator {
    private static final Set<Class> RX_SUPPORTED_TYPES = newHashSet(ObservableSource.class, SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

    @SuppressWarnings("unchecked")
    @Override
    public boolean supports(Class<?> target) {
        return RX_SUPPORTED_TYPES.stream().anyMatch(it -> it.isAssignableFrom(target));
    }

    @Override
    public CheckedFunction0<Object> decorate(RecoveryMethod recoveryMethod, CheckedFunction0<Object> supplier) {
        return supplier.andThen(request -> {
            if (request instanceof ObservableSource) {
                Observable<?> observable = (Observable<?>) request;
                return observable.onErrorResumeNext(rxJava2OnErrorResumeNext(recoveryMethod, Observable::error));
            } else if (request instanceof SingleSource) {
                Single<?> single = (Single) request;
                return single.onErrorResumeNext(rxJava2OnErrorResumeNext(recoveryMethod, Single::error));
            } else if (request instanceof CompletableSource) {
                Completable completable = (Completable) request;
                return completable.onErrorResumeNext(rxJava2OnErrorResumeNext(recoveryMethod, Completable::error));
            } else if (request instanceof MaybeSource) {
                Maybe<?> maybe = (Maybe) request;
                return maybe.onErrorResumeNext(rxJava2OnErrorResumeNext(recoveryMethod, Maybe::error));
            } else if (request instanceof Flowable) {
                Flowable<?> flowable = (Flowable) request;
                return flowable.onErrorResumeNext(rxJava2OnErrorResumeNext(recoveryMethod, Flowable::error));
            } else {
                return request;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> io.reactivex.functions.Function<Throwable, T> rxJava2OnErrorResumeNext(RecoveryMethod recoveryMethod, Function<? super Throwable, ? extends T> errorFunction) {
        return (throwable) -> {
            try {
                return (T) recoveryMethod.recover(throwable);
            } catch (Throwable recoverThrowable) {
                return (T) errorFunction.apply(recoverThrowable);
            }
        };
    }
}
