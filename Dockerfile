##STAGE 1 -> BUILD

# Environment used to compile the Gateway.
FROM ubuntu:22.04 AS builder


# Install the tools required for the build process.
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    ninja-build \
    git \
    curl \
    zip \
    unzip \
    tar \
    pkg-config \
    python3 \
    && rm -rf /var/lib/apt/lists/*


# Install vcpkg to manage the C++ dependencies.
RUN git clone https://github.com/microsoft/vcpkg.git /vcpkg \
    && /vcpkg/bootstrap-vcpkg.sh


# Configure vcpkg environment variables.
ENV VCPKG_ROOT=/vcpkg
ENV PATH="${VCPKG_ROOT}:${PATH}"

WORKDIR /src


# Copy the files required to configure and build the Gateway.
COPY gateway/vcpkg.json .
COPY gateway/CMakeLists.txt .
COPY gateway/src ./src
COPY gateway/include ./include
COPY gateway/proto ./proto


# Configure and compile the Gateway.
RUN cmake -B build -S . \
    -DCMAKE_TOOLCHAIN_FILE=/vcpkg/scripts/buildsystems/vcpkg.cmake \
    && cmake --build build




## STAGE 2 -> RUNTIME


FROM ubuntu:22.04

WORKDIR /src

# Copy only the compiled Gateway from the build stage.
COPY --from=builder /src/build/gateway /src/build/gateway

# Start the Gateway when the container launches.
CMD ["/src/build/gateway"]